package com.enryold.onioncache.services;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.enryold.onioncache.interfaces.ICacheLayerDataModel;
import com.enryold.onioncache.interfaces.ICacheLayerMarshaller;
import com.enryold.onioncache.interfaces.ICacheLayerService;

import java.util.Optional;


public class DynamoDBService implements ICacheLayerService {

    DynamoDBMapper dynamoDBMapper;




    public DynamoDBService(AmazonDynamoDB amazonDynamoDB)
    {
        dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB);
    }

    @Override
    public boolean set(ICacheLayerDataModel value, ICacheLayerMarshaller marshaller) {
        Optional m = marshaller.marshall(value);
        dynamoDBMapper.save(m.get());
        return m.isPresent();
    }

    @Override
    public boolean setEx(ICacheLayerDataModel value, int expiration, ICacheLayerMarshaller marshaller) {
        return this.set(value, marshaller);
    }

    @Override
    public Optional get(ICacheLayerDataModel value, ICacheLayerMarshaller marshaller) {
        return marshaller.unMarshall(dynamoDBMapper.load(value));
    }

    @Override
    public boolean delete(ICacheLayerDataModel value) {
        dynamoDBMapper.delete(value);
        return true;
    }





}
