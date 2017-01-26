package com.enryold.onioncache;


import com.enryold.onioncache.interfaces.*;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by enryold on 16/01/17.
 */
public class CacheLayer<S extends ICacheLayerService, M extends ICacheLayerMarshaller<T,?>, T extends ICacheLayerDataModel> implements ICacheLayer<S, M, T> {

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
    public boolean set(T t) {
        return service.set(t, marshaller);
    }


    public boolean set(T t, int expire) {
        return service.setEx(t, expire, marshaller);
    }

    @Override
    public Optional<T> get(T t) {
        return service.get(t, marshaller);
    }


    public boolean delete(T t) {
        return service.delete(t);
    }
}
