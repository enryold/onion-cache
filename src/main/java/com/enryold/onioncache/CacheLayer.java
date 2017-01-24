package com.enryold.onioncache;


import com.enryold.onioncache.interfaces.ICacheLayer;
import com.enryold.onioncache.interfaces.ICacheLayerKey;
import com.enryold.onioncache.interfaces.ICacheLayerMarshaller;
import com.enryold.onioncache.interfaces.ICacheLayerService;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by enryold on 16/01/17.
 */
public class CacheLayer<S extends ICacheLayerService, M extends ICacheLayerMarshaller<T,?>, T> implements ICacheLayer<S, M, T> {

    private S service;
    private int defaultExpiration;
    private ICacheLayerMarshaller marshaller;



    @Override
    public CacheLayer<S, M ,T> withMainService(S service) {
        this.service = service;
        return this;
    }

    @Override
    public CacheLayer<S, M, T> withMainServiceMarshaller(ICacheLayerMarshaller marshaller) {
        this.marshaller = marshaller;
        return this;
    }


    @Override
    public CacheLayer<S, M, T> withDefaultExpiration(int defaultExpiration) {
        this.defaultExpiration = defaultExpiration;
        return this;
    }





    @Override
    public boolean set(T t, String hashKey, String rangeKey, int expire) {
        return service.setEx(hashKey, rangeKey, t, expire, marshaller) != null;
    }

    @Override
    public boolean set(T t, String hashKey, int expire) {
        return this.set(t, hashKey, null, expire);
    }

    @Override
    public boolean set(T t, String hashKey) {
        return this.set(t, hashKey, null, defaultExpiration);
    }

    @Override
    public boolean set(T t, String hashKey, String rangeKey) {
        return this.set(t, hashKey, rangeKey, defaultExpiration);
    }

    @Override
    public Optional<T> get(String hashKey) {
        return this.get(hashKey, null);
    }

    @Override
    public Optional<T> get(String hashKey, String rangeKey) {
        return service.get(hashKey, rangeKey, marshaller);
    }

    @Override
    public boolean delete(String hashKey) {
        return this.delete(hashKey, null);
    }

    @Override
    public boolean delete(String hashKey, String rangeKey) {
        return this.delete(hashKey, rangeKey);
    }
}
