# onion-cache 

[![Build Status](https://travis-ci.org/enryold/onion-cache.svg?branch=develop)](https://travis-ci.org/enryold/onion-cache)


# Multilevel cache system for Java 8 




## Goal:

##### Build a cache system:

- That makes simple add/remove different layers of cache.
- With a system that fallback through one or multiple cache layers before getting the needed object from the main datasource.


##### How I design it:

- I try to generealize the concept of Cache Layer in order to write less logic as possible.
- I came up with 3 main entity that describes a Cache Layer.
  + A service, which represent a client wrapper for cache/datastore service in real world. It must implement 4 basic methods: GET, SET, SETEX, DELETE.
  + A cacheLayerKey which represent an object that build you key. I use 2 levels of deepness for keys. Hash and Range (Range is optional).
    In this way you can implement your own RedisService that can use HSET instead of SET if a range key is setted.
  + A marshaller, which marshall/unmarshall the data you want to cache. It must implement 2 methods: marshall and unMarshall.
- OnionCache addLayer() method will insert a cache layer. The last layer will be the main datasource.
  + A typical use case will be (in order!):
      addLayer(LRU);
      addLayer(Redis);
      addLayer(MySQL);
- You can extend it building new services, keys, marshallers.
- At the moment some services are present: LRUCache, Redis, DynamoDb, FileSystem (this is useful if you use as main datastore a network file system like Amazon NFS)


##### Little Hack:

My DynamoDbService uses internally DynamodbMapper which is a great way to map your existing model with annotations.
    In this case your model knows what properties are Hash and Range keys because they are specified into model itself.
    As you can see in src/test/OnionCacheTest.java I not specify any CacheLayerKey and any Marshaller for this kind of layer.
    Of course any SET/SETEX call will skip hash and range key parameters, because they are defined in object itself.

    This is userful for different ORM. 

##### Let's start:

Imagine a system that saves data on DynamoDb, but some datas are very popular in certain period of time with an increase of throughput-rate.
In this case is useful to put Redis or Memcached between your webapp and DynamoDb and an LRU cache inside your application to avoid any network call.

DRAW.IO

First of all we have to create all the layers we need in our system design, with their services, keys, and marshallers

```java
// THIS EXAMPLE CAN BE FOUND UNDER /test FOLDER IN METHOD NAMED test_LRU_REDIS_DYNAMODB_FALLBACK()


// SERVICES FIRST

// IN MEMORY LRU SERVICE, 100 is the size of the LRU map. 
InMemoryLRUService inMemoryLRUService = new InMemoryLRUService(100);
inMemoryLRUService.withKeyFunction(new CacheLayerKey());

// REDIS SERVICE
RedisService redisService = new RedisService("localhost", 6379);
redisService.withKeyFunction(new CacheLayerKey());

// DYNAMODB SERVICE
DynamoDBService dynamoDBService = new DynamoDBService(dynamodb);

// NOW BUILD CACHE LAYERS FOR THE MODEL Person, WITH REPRESENT A PERSON WITH NAME AND SURNAME PROPERTIES.
CacheLayer LRULayer = new CacheLayer<InMemoryLRUService, CacheLayerJsonMarshaller<Person>, Person>()
    .withMainService(inMemoryLRUService)
    .withMainServiceMarshaller(new CacheLayerJsonMarshaller<>(Person.class, String.class));

CacheLayer DynamoDBLayer = new CacheLayer<DynamoDBService, CacheLayerDynamoDBMarshaller<Person>, Person>()
    .withMainService(dynamoDBService)
    .withMainServiceMarshaller(new CacheLayerDynamoDBMarshaller<>(Person.class, Person.class));

// IN REDIS WE CAN SET DEFAULT EXPIRATION FOR KEYS IN SECONDS
CacheLayer redisLayer = new CacheLayer<RedisService, CacheLayerJsonMarshaller<Person>, Person>()
    .withMainService(redisService)
    .withMainServiceMarshaller(new CacheLayerJsonMarshaller<>(Person.class, String.class))
    .withDefaultExpiration(3600);

// NOW WE ARE READY TO BUILD OUR ONIONCACHE OBJECT, THE ORDER OF addLayer IS IMPORTANT!!
OnionCache<Person> onionCache = new OnionCache<Person>()
    .addLayer(LRULayer)
    .addLayer(redisLayer)
    .addLayer(DynamoDBLayer);
    
// THEN, WE CAN USER ONIONCACHE TO RETRIEVE OUR OBJECTS FROM THE LAYERS. 
Optional<Person> object = onionCache.get(goJason.getName(), goJason.getSurname());

// HOWEVER, WE CAN ALSO SET A NEW OBJECT, WITH CUSTOM DEFAULT EXPIRATION (This works only in Redis/Memcached)
Person goJason = new Person("Jason", "Bourne");
boolean success = onionCache.set(goJason.getName(), goJason.getSurname(), goJason, 30 );


```


