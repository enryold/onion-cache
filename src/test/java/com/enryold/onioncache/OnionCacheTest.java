package com.enryold.onioncache;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.amazonaws.services.dynamodbv2.model.*;
import com.enryold.onioncache.marshallers.OnionCacheLayerDynamoDBMarshaller;
import com.enryold.onioncache.marshallers.OnionCacheLayerJsonMarshaller;
import com.enryold.onioncache.models.Person;
import com.enryold.onioncache.services.OnionDynamoDBService;
import com.enryold.onioncache.services.OnionFileSystemService;
import com.enryold.onioncache.services.OnionInMemoryLRUService;
import com.enryold.onioncache.services.OnionRedisService;
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
        OnionRedisService redisService = new OnionRedisService("localhost", REDIS_PORT);

        OnionInMemoryLRUService inMemoryLRUService = new OnionInMemoryLRUService(100);



        // Setup cache layers (Key function, Cache service, Model)
        OnionCacheLayer LRULayer = new OnionCacheLayer<OnionInMemoryLRUService, OnionCacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(inMemoryLRUService)
                .withMainServiceMarshaller(new OnionCacheLayerJsonMarshaller<>(Person.class, String.class));



        OnionCacheLayer redisLayer = new OnionCacheLayer<OnionRedisService, OnionCacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(redisService)
                .withMainServiceMarshaller(new OnionCacheLayerJsonMarshaller<>(Person.class, String.class));


        // Test with fake model Person
        OnionCache<Person> onionCache = new OnionCache<Person>()
                .addLayer(LRULayer)
                .addLayer(redisLayer);

        Person goJason = new Person("Jason", "Bourne");

        boolean setResult = onionCache.set(goJason, 30 );

        // Check if result is true
        Assert.assertTrue(setResult);

        // Check both service for object
        Assert.assertTrue(inMemoryLRUService.get(new Person("Jason", "Bourne"), new OnionCacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());
        Assert.assertTrue(redisService.get(new Person("Jason", "Bourne"), new OnionCacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());

        // Get Object Again
        Optional<Person> object = onionCache.get(new Person("Jason", "Bourne"));

        Assert.assertTrue(object.isPresent());
        Assert.assertTrue(object.get().getName().equals(goJason.getName()));
        Assert.assertTrue(object.get().getSurname().equals(goJason.getSurname()));


    }





    @Test
    public void test_LRU_FILESYSTEM_FALLBACK()
    {
        OnionInMemoryLRUService inMemoryLRUService = new OnionInMemoryLRUService(100);


        try
        {
            OnionFileSystemService onionFileSystemService = new OnionFileSystemService("build/tmp");



        // Setup cache layers (Cache service, Marshaller, Model)
        OnionCacheLayer LRULayer = new OnionCacheLayer<OnionInMemoryLRUService, OnionCacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(inMemoryLRUService)
                .withMainServiceMarshaller(new OnionCacheLayerJsonMarshaller<>(Person.class, String.class));


        OnionCacheLayer fsLayer = new OnionCacheLayer<OnionFileSystemService, OnionCacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(onionFileSystemService)
                .withMainServiceMarshaller(new OnionCacheLayerJsonMarshaller<>(Person.class, String.class));




        // Test with fake model Person
        OnionCache<Person> onionCache = new OnionCache<Person>()
                .addLayer(LRULayer)
                .addLayer(fsLayer);

        Person goJason = new Person("Jason", "Bourne");

        boolean setResult = onionCache.set(goJason, 30 );

        // Check if result is true
        Assert.assertTrue(setResult);

        // Check both service for object
        Assert.assertTrue(inMemoryLRUService.get(new Person("Jason", "Bourne"), new OnionCacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());
        Assert.assertTrue(onionFileSystemService.get(new Person("Jason", "Bourne"), new OnionCacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());

        // Get Object Again
        Optional<Person> object = onionCache.get(new Person("Jason", "Bourne"));

        Assert.assertTrue(object.isPresent());
        Assert.assertTrue(object.get().getName().equals(goJason.getName()));
        Assert.assertTrue(object.get().getSurname().equals(goJason.getSurname()));

        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }

    }



    @Test
    public void test_LRU_FILESYSTEM_FALLBACK_WITH_FILENOTFOUND()
    {
        OnionInMemoryLRUService inMemoryLRUService = new OnionInMemoryLRUService(100);


        try
        {
            OnionFileSystemService onionFileSystemService = new OnionFileSystemService("build/tmp");
            onionFileSystemService.delete(new Person("Jason", "Bourne"));


            // Setup cache layers (Cache service, Marshaller, Model)
            OnionCacheLayer LRULayer = new OnionCacheLayer<OnionInMemoryLRUService, OnionCacheLayerJsonMarshaller<Person>, Person>()
                    .withMainService(inMemoryLRUService)
                    .withMainServiceMarshaller(new OnionCacheLayerJsonMarshaller<>(Person.class, String.class));


            OnionCacheLayer fsLayer = new OnionCacheLayer<OnionFileSystemService, OnionCacheLayerJsonMarshaller<Person>, Person>()
                    .withMainService(onionFileSystemService)
                    .withMainServiceMarshaller(new OnionCacheLayerJsonMarshaller<>(Person.class, String.class));




            // Test with fake model Person
            OnionCache<Person> onionCache = new OnionCache<Person>()
                    .addLayer(LRULayer)
                    .addLayer(fsLayer);


            Assert.assertTrue(!onionFileSystemService.get(new Person("Jason", "Bourne"), new OnionCacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());


            Person goJason = new Person("Jason", "Bourne");

            boolean setResult = onionCache.set(goJason, 30 );

            // Check if result is true
            Assert.assertTrue(setResult);

            // Check both service for object
            Assert.assertTrue(inMemoryLRUService.get(new Person("Jason", "Bourne"), new OnionCacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());
            Assert.assertTrue(onionFileSystemService.get(new Person("Jason", "Bourne"), new OnionCacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());

            // Get Object Again
            Optional<Person> object = onionCache.get(new Person("Jason", "Bourne"));

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
        OnionRedisService redisService = new OnionRedisService("localhost", REDIS_PORT);

        OnionDynamoDBService onionDynamoDBService = new OnionDynamoDBService(dynamodb);


        // Setup cache layers (Key function, Cache service, Model)
        /** THIS IS A FAKE MARSHALLER CAUSE DYNAMO-DB ALREADY HAVE HIS MARSHALLER */
        OnionCacheLayer DynamoDBLayer = new OnionCacheLayer<OnionDynamoDBService, OnionCacheLayerDynamoDBMarshaller<Person>, Person>()
                .withMainService(onionDynamoDBService)
                .withMainServiceMarshaller(new OnionCacheLayerDynamoDBMarshaller<>(Person.class, Person.class));



        OnionCacheLayer redisLayer = new OnionCacheLayer<OnionRedisService, OnionCacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(redisService)
                .withMainServiceMarshaller(new OnionCacheLayerJsonMarshaller<>(Person.class, String.class));


        // Test with fake model Person
        OnionCache<Person> onionCache = new OnionCache<Person>()
                .addLayer(redisLayer)
                .addLayer(DynamoDBLayer);

        Person goJason = new Person("Jason", "Bourne");

        boolean setResult = onionCache.set(goJason, 30 );

        // Check if result is true
        Assert.assertTrue(setResult);

        // Check both service for object
        Assert.assertTrue(onionDynamoDBService.get(new Person("Jason", "Bourne"), new OnionCacheLayerDynamoDBMarshaller<>(Person.class, Person.class)).isPresent());
        Assert.assertTrue(redisService.get(new Person("Jason", "Bourne"), new OnionCacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());

        // Get Object Again
        Optional<Person> object = onionCache.get(new Person("Jason", "Bourne"));

        Assert.assertTrue(object.isPresent());
        Assert.assertTrue(object.get().getName().equals(goJason.getName()));
        Assert.assertTrue(object.get().getSurname().equals(goJason.getSurname()));


    }


    @Test
    public void test_LRU_REDIS_DYNAMODB_FALLBACK()
    {
        OnionInMemoryLRUService inMemoryLRUService = new OnionInMemoryLRUService(100);
        OnionRedisService redisService = new OnionRedisService("localhost", REDIS_PORT);
        OnionDynamoDBService onionDynamoDBService = new OnionDynamoDBService(dynamodb);


        // Setup cache layers (Key function, Cache service, Model)
        /** THIS IS A FAKE MARSHALLER CAUSE DYNAMO-DB ALREADY HAVE HIS MARSHALLER */
        OnionCacheLayer DynamoDBLayer = new OnionCacheLayer<OnionDynamoDBService, OnionCacheLayerDynamoDBMarshaller<Person>, Person>()
                .withMainService(onionDynamoDBService)
                .withMainServiceMarshaller(new OnionCacheLayerDynamoDBMarshaller<>(Person.class, Person.class));


        // Setup cache layers (Cache service, Marshaller, Model)
        OnionCacheLayer LRULayer = new OnionCacheLayer<OnionInMemoryLRUService, OnionCacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(inMemoryLRUService)
                .withMainServiceMarshaller(new OnionCacheLayerJsonMarshaller<>(Person.class, String.class));


        OnionCacheLayer redisLayer = new OnionCacheLayer<OnionRedisService, OnionCacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(redisService)
                .withMainServiceMarshaller(new OnionCacheLayerJsonMarshaller<>(Person.class, String.class));


        // Test with fake model Person
        OnionCache<Person> onionCache = new OnionCache<Person>()
                .addLayer(LRULayer)
                .addLayer(redisLayer)
                .addLayer(DynamoDBLayer);

        Person goJason = new Person("Jason", "Bourne");

        boolean setResult = onionCache.set(goJason, 30 );

        // Check if result is true
        Assert.assertTrue(setResult);

        // Check all services for object
        Assert.assertTrue(onionDynamoDBService.get(new Person("Jason", "Bourne"), new OnionCacheLayerDynamoDBMarshaller<>(Person.class, Person.class)).isPresent());
        Assert.assertTrue(redisService.get(new Person("Jason", "Bourne"), new OnionCacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());
        Assert.assertTrue(inMemoryLRUService.get(new Person("Jason", "Bourne"), new OnionCacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());

        // Get Object Again
        Optional<Person> object = onionCache.get(new Person("Jason", "Bourne"));

        Assert.assertTrue(object.isPresent());
        Assert.assertTrue(object.get().getName().equals(goJason.getName()));
        Assert.assertTrue(object.get().getSurname().equals(goJason.getSurname()));


    }


    @Test
    public void test_REDIS_DYNAMODB_EXPIRATION_FALLBACK()
    {
        OnionRedisService redisService = new OnionRedisService("localhost", REDIS_PORT);

        OnionDynamoDBService onionDynamoDBService = new OnionDynamoDBService(dynamodb);



        // Setup cache layers (Key function, Cache service, Model)
        /** THIS IS A FAKE MARSHALLER CAUSE DYNAMO-DB ALREADY HAVE HIS MARSHALLER */
        OnionCacheLayer DynamoDBLayer = new OnionCacheLayer<OnionDynamoDBService, OnionCacheLayerDynamoDBMarshaller<Person>, Person>()
                .withMainService(onionDynamoDBService)
                .withMainServiceMarshaller(new OnionCacheLayerDynamoDBMarshaller<>(Person.class, Person.class));



        OnionCacheLayer redisLayer = new OnionCacheLayer<OnionRedisService, OnionCacheLayerJsonMarshaller<Person>, Person>()
                .withMainService(redisService)
                .withMainServiceMarshaller(new OnionCacheLayerJsonMarshaller<>(Person.class, String.class))
                .withDefaultExpiration(60);


        // Test with fake model Person
        OnionCache<Person> onionCache = new OnionCache<Person>()
                .addLayer(redisLayer)
                .addLayer(DynamoDBLayer);

        Person goJason = new Person("Jason", "Bourne");

        boolean setResult = onionCache.set(goJason, 5);

        // Check if result is true
        Assert.assertTrue(setResult);

        // Check both service for object
        Assert.assertTrue(redisService.get(new Person("Jason", "Bourne"), new OnionCacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());
        Assert.assertTrue(onionDynamoDBService.get(new Person("Jason", "Bourne"), new OnionCacheLayerDynamoDBMarshaller<>(Person.class, Person.class)).isPresent());

        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Redis was expired
        Assert.assertTrue(!redisService.get(new Person("Jason", "Bourne"), new OnionCacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());

        // Dynamo still have object
        Assert.assertTrue(onionDynamoDBService.get(new Person("Jason", "Bourne"), new OnionCacheLayerDynamoDBMarshaller<>(Person.class, Person.class)).isPresent());



        // Get Object Again - This call put again object into redis.
        Optional<Person> object = onionCache.get(new Person("Jason", "Bourne"));

        // Redis now have object
        Assert.assertTrue(redisService.get(new Person("Jason", "Bourne"), new OnionCacheLayerJsonMarshaller<>(Person.class, String.class)).isPresent());


        Assert.assertTrue(object.isPresent());
        Assert.assertTrue(object.get().getName().equals(goJason.getName()));
        Assert.assertTrue(object.get().getSurname().equals(goJason.getSurname()));


    }

}
