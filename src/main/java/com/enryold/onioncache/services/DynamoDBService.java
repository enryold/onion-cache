package com.enryold.onioncache.services;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.enryold.onioncache.CacheLayerKey;
import com.enryold.onioncache.interfaces.ICacheLayerMarshaller;
import com.enryold.onioncache.interfaces.ICacheLayerService;

import java.util.Optional;


public class DynamoDBService implements ICacheLayerService<CacheLayerKey> {

    DynamoDBMapper dynamoDBMapper;
    private CacheLayerKey cacheLayerKey;




    public DynamoDBService(AmazonDynamoDB amazonDynamoDB)
    {
        dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB);
    }


    @Override
    public Object set(String hashKey, String rangeKey, Object value, ICacheLayerMarshaller marshaller) {
        dynamoDBMapper.save(value);
        return value;
    }

    @Override
    public Object setEx(String hashKey, String rangeKey, Object value, int expiration, ICacheLayerMarshaller marshaller) {
        return this.set(hashKey, rangeKey, value, marshaller);
    }

    @Override
    public Optional<Object> get(String hashKey, String rangeKey, ICacheLayerMarshaller marshaller)
    {
        Object result = dynamoDBMapper.load(marshaller.getOutputClazz(), hashKey, rangeKey);
        return Optional.ofNullable(result);
    }

    @Override
    public boolean delete(String hashKey, String rangeKey) {
        return false;
    }

    @Override
    public ICacheLayerService<CacheLayerKey> withKeyFunction(CacheLayerKey k) {
        this.cacheLayerKey = k;
        return this;
    }




}
