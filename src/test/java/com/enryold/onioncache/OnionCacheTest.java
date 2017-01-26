package com.enryold.onioncache;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.xspec.L;
import com.enryold.onioncache.marshallers.CacheLayerDynamoDBMarshaller;
import com.enryold.onioncache.marshallers.CacheLayerJsonMarshaller;
import com.enryold.onioncache.models.Person;
import com.enryold.onioncache.services.DynamoDBService;
import com.enryold.onioncache.services.FileSystemService;
import com.enryold.onioncache.services.InMemoryLRUService;
import com.enryold.onioncache.services.RedisService;
import org.apache.commons.cli.ParseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Created by enryold on 17/01/17.
 */


public class OnionCacheTest
{
    public static final int REDIS_PORT = 6379;
    public static final String FAKE_HASH_KEY = "hashKey";
    public static final String FAKE_RANGE_KEY = "rangeKey";

    private RedisServer redisServer;
    private AmazonDynamoDBClient dynamodb;
    private DynamoDBProxyServer server;



    @Before
    public void init() {


        try {
            redisServer = new RedisServer(REDIS_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try {
            redisServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }







        final String[] localArgs = { "-inMemory" };
        try {
            server = ServerRunner.createServerFromCommandLineArgs(localArgs);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        dynamodb = new AmazonDynamoDBClient(new BasicAWSCredentials("fake", "fake"));
        dynamodb.setEndpoint("http://localhost:8000");
        this.dynamoDBLocalCreateTable(Person.class);


    }

    @After
    public void destroy(){

        try {
            redisServer.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void dynamoDBLocalCreateTable(Class clazz)
    {
        try {

            DynamoDBMapper mapper = new DynamoDBMapper(dynamodb);
            CreateTableRequest request = mapper.generateCreateTableRequest(clazz);
            request.setProvisionedThroughput(new ProvisionedThroughput()
                    .withReadCapacityUnits(10L)
                    .withWriteCapacityUnits(10L));
            CreateTableResult r = dynamodb.createTable(request);

        } catch (ResourceInUseException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @Test
    public void test_LRU_REDIS_FALLBACK()
    {
        RedisService redisService = new RedisService("localhost", REDIS_PORT);
        redisService.withKeyFunction(new CacheLayerKey());

        InMemoryLRUService inMemoryLRUService = new InMemoryLRUService(100);
        inMemoryLRUService.withKeyFunction(new CacheLayerKey());




        // Setup cache layers (Key function, Cache service, Model)
        CacheLayer LRULayer = new CacheLayer<InMemoryLRUService, CacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(inMemoryLRUService)
                .withMainServiceMarshaller(new CacheLayerJsonMarshaller<>(Person.class, String.class));



        CacheLayer redisLayer = new CacheLayer<RedisService, CacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(redisService)
                .withMainServiceMarshaller(new CacheLayerJsonMarshaller<>(Person.class, String.class));


        // Test with fake model Person
        OnionCache<Person> onionCache = new OnionCache<Person>()
                .addLayer(LRULayer)
                .addLayer(redisLayer);

        Person goJason = new Person("Jason", "Bourne");

        boolean setResult = onionCache.set(FAKE_HASH_KEY, FAKE_RANGE_KEY, goJason, 30 );

        // Check if result is true
        Assert.assertTrue(setResult);

        // Check both service for object
        Assert.assertTrue(inMemoryLRUService.get(FAKE_HASH_KEY, FAKE_RANGE_KEY, new CacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());
        Assert.assertTrue(redisService.get(FAKE_HASH_KEY, FAKE_RANGE_KEY, new CacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());

        // Get Object Again
        Optional<Person> object = onionCache.get(FAKE_HASH_KEY, FAKE_RANGE_KEY);

        Assert.assertTrue(object.isPresent());
        Assert.assertTrue(object.get().getName().equals(goJason.getName()));
        Assert.assertTrue(object.get().getSurname().equals(goJason.getSurname()));


    }





    @Test
    public void test_LRU_FILESYSTEM_FALLBACK()
    {
        InMemoryLRUService inMemoryLRUService = new InMemoryLRUService(100);
        inMemoryLRUService.withKeyFunction(new CacheLayerKey());


        try
        {
            FileSystemService fileSystemService = new FileSystemService("build/tmp");
            fileSystemService.withKeyFunction(new CacheLayerKey());



        // Setup cache layers (Cache service, Marshaller, Model)
        CacheLayer LRULayer = new CacheLayer<InMemoryLRUService, CacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(inMemoryLRUService)
                .withMainServiceMarshaller(new CacheLayerJsonMarshaller<>(Person.class, String.class));


        CacheLayer fsLayer = new CacheLayer<FileSystemService, CacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(fileSystemService)
                .withMainServiceMarshaller(new CacheLayerJsonMarshaller<>(Person.class, String.class));




        // Test with fake model Person
        OnionCache<Person> onionCache = new OnionCache<Person>()
                .addLayer(LRULayer)
                .addLayer(fsLayer);

        Person goJason = new Person("Jason", "Bourne");

        boolean setResult = onionCache.set(FAKE_HASH_KEY, FAKE_RANGE_KEY, goJason, 30 );

        // Check if result is true
        Assert.assertTrue(setResult);

        // Check both service for object
        Assert.assertTrue(inMemoryLRUService.get(FAKE_HASH_KEY, FAKE_RANGE_KEY, new CacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());
        Assert.assertTrue(fileSystemService.get(FAKE_HASH_KEY, FAKE_RANGE_KEY, new CacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());

        // Get Object Again
        Optional<Person> object = onionCache.get(FAKE_HASH_KEY, FAKE_RANGE_KEY);

        Assert.assertTrue(object.isPresent());
        Assert.assertTrue(object.get().getName().equals(goJason.getName()));
        Assert.assertTrue(object.get().getSurname().equals(goJason.getSurname()));

        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }

    }



    @Test
    public void test_REDIS_DYNAMODB_FALLBACK()
    {
        RedisService redisService = new RedisService("localhost", REDIS_PORT);
        redisService.withKeyFunction(new CacheLayerKey());

        DynamoDBService dynamoDBService = new DynamoDBService(dynamodb);


        // Setup cache layers (Key function, Cache service, Model)
        /** THIS IS A FAKE MARSHALLER CAUSE DYNAMO-DB ALREADY HAVE HIS MARSHALLER */
        CacheLayer DynamoDBLayer = new CacheLayer<DynamoDBService, CacheLayerDynamoDBMarshaller<Person>, Person>()
                .withMainService(dynamoDBService)
                .withMainServiceMarshaller(new CacheLayerDynamoDBMarshaller<>(Person.class, Person.class));



        CacheLayer redisLayer = new CacheLayer<RedisService, CacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(redisService)
                .withMainServiceMarshaller(new CacheLayerJsonMarshaller<>(Person.class, String.class));


        // Test with fake model Person
        OnionCache<Person> onionCache = new OnionCache<Person>()
                .addLayer(redisLayer)
                .addLayer(DynamoDBLayer);

        Person goJason = new Person("Jason", "Bourne");

        boolean setResult = onionCache.set(goJason.getName(), goJason.getSurname(), goJason, 30 );

        // Check if result is true
        Assert.assertTrue(setResult);

        // Check both service for object
        Assert.assertTrue(dynamoDBService.get(goJason.getName(), goJason.getSurname(), new CacheLayerDynamoDBMarshaller<>(Person.class, Person.class)).isPresent());
        Assert.assertTrue(redisService.get(goJason.getName(), goJason.getSurname(), new CacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());

        // Get Object Again
        Optional<Person> object = onionCache.get(goJason.getName(), goJason.getSurname());

        Assert.assertTrue(object.isPresent());
        Assert.assertTrue(object.get().getName().equals(goJason.getName()));
        Assert.assertTrue(object.get().getSurname().equals(goJason.getSurname()));


    }


    @Test
    public void test_LRU_REDIS_DYNAMODB_FALLBACK()
    {
        InMemoryLRUService inMemoryLRUService = new InMemoryLRUService(100);
        inMemoryLRUService.withKeyFunction(new CacheLayerKey());

        RedisService redisService = new RedisService("localhost", REDIS_PORT);
        redisService.withKeyFunction(new CacheLayerKey());

        DynamoDBService dynamoDBService = new DynamoDBService(dynamodb);


        // Setup cache layers (Key function, Cache service, Model)
        /** THIS IS A FAKE MARSHALLER CAUSE DYNAMO-DB ALREADY HAVE HIS MARSHALLER */
        CacheLayer DynamoDBLayer = new CacheLayer<DynamoDBService, CacheLayerDynamoDBMarshaller<Person>, Person>()
                .withMainService(dynamoDBService)
                .withMainServiceMarshaller(new CacheLayerDynamoDBMarshaller<>(Person.class, Person.class));


        // Setup cache layers (Cache service, Marshaller, Model)
        CacheLayer LRULayer = new CacheLayer<InMemoryLRUService, CacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(inMemoryLRUService)
                .withMainServiceMarshaller(new CacheLayerJsonMarshaller<>(Person.class, String.class));


        CacheLayer redisLayer = new CacheLayer<RedisService, CacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(redisService)
                .withMainServiceMarshaller(new CacheLayerJsonMarshaller<>(Person.class, String.class));


        // Test with fake model Person
        OnionCache<Person> onionCache = new OnionCache<Person>()
                .addLayer(LRULayer)
                .addLayer(redisLayer)
                .addLayer(DynamoDBLayer);

        Person goJason = new Person("Jason", "Bourne");

        boolean setResult = onionCache.set(goJason.getName(), goJason.getSurname(), goJason, 30 );

        // Check if result is true
        Assert.assertTrue(setResult);

        // Check all services for object
        Assert.assertTrue(dynamoDBService.get(goJason.getName(), goJason.getSurname(), new CacheLayerDynamoDBMarshaller<>(Person.class, Person.class)).isPresent());
        Assert.assertTrue(redisService.get(goJason.getName(), goJason.getSurname(), new CacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());
        Assert.assertTrue(inMemoryLRUService.get(goJason.getName(), goJason.getSurname(), new CacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());

        // Get Object Again
        Optional<Person> object = onionCache.get(goJason.getName(), goJason.getSurname());

        Assert.assertTrue(object.isPresent());
        Assert.assertTrue(object.get().getName().equals(goJason.getName()));
        Assert.assertTrue(object.get().getSurname().equals(goJason.getSurname()));


    }


    @Test
    public void test_REDIS_DYNAMODB_EXPIRATION_FALLBACK()
    {
        RedisService redisService = new RedisService("localhost", REDIS_PORT);
        redisService
                .withKeyFunction(new CacheLayerKey());

        DynamoDBService dynamoDBService = new DynamoDBService(dynamodb);



        // Setup cache layers (Key function, Cache service, Model)
        /** THIS IS A FAKE MARSHALLER CAUSE DYNAMO-DB ALREADY HAVE HIS MARSHALLER */
        CacheLayer DynamoDBLayer = new CacheLayer<DynamoDBService, CacheLayerDynamoDBMarshaller<Person>, Person>()
                .withMainService(dynamoDBService)
                .withMainServiceMarshaller(new CacheLayerDynamoDBMarshaller<>(Person.class, Person.class));



        CacheLayer redisLayer = new CacheLayer<RedisService, CacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(redisService)
                .withMainServiceMarshaller(new CacheLayerJsonMarshaller<>(Person.class, String.class))
                .withDefaultExpiration(60);


        // Test with fake model Person
        OnionCache<Person> onionCache = new OnionCache<Person>()
                .addLayer(redisLayer)
                .addLayer(DynamoDBLayer);

        Person goJason = new Person("Jason", "Bourne");

        boolean setResult = onionCache.set(goJason.getName(), goJason.getSurname(), goJason, 5);

        // Check if result is true
        Assert.assertTrue(setResult);

        // Check both service for object
        Assert.assertTrue(redisService.get(goJason.getName(), goJason.getSurname(), new CacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());
        Assert.assertTrue(dynamoDBService.get(goJason.getName(), goJason.getSurname(), new CacheLayerDynamoDBMarshaller<>(Person.class, Person.class)).isPresent());

        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Redis was expired
        Assert.assertTrue(!redisService.get(goJason.getName(), goJason.getSurname(), new CacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());

        // Dynamo still have object
        Assert.assertTrue(dynamoDBService.get(goJason.getName(), goJason.getSurname(), new CacheLayerDynamoDBMarshaller<>(Person.class, Person.class)).isPresent());



        // Get Object Again - This call put again object into redis.
        Optional<Person> object = onionCache.get(goJason.getName(), goJason.getSurname());

        // Redis now have object
        Assert.assertTrue(redisService.get(goJason.getName(), goJason.getSurname(), new CacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());


        Assert.assertTrue(object.isPresent());
        Assert.assertTrue(object.get().getName().equals(goJason.getName()));
        Assert.assertTrue(object.get().getSurname().equals(goJason.getSurname()));


    }

}
