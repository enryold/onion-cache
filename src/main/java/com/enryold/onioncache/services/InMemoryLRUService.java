package com.enryold.onioncache.services;


import com.enryold.onioncache.CacheLayerKey;
import com.enryold.onioncache.interfaces.ICacheLayerMarshaller;
import com.enryold.onioncache.interfaces.ICacheLayerService;
import com.enryold.onioncache.utils.LRUMap;

import java.util.Optional;


public class InMemoryLRUService implements ICacheLayerService<CacheLayerKey> {

    private LRUMap<String, Object> cache;
    private CacheLayerKey cacheLayerKey;




    public InMemoryLRUService(int capacity)
    {
        cache = new LRUMap<>(capacity);
    }


    @Override
    public Object set(String hashKey, String rangeKey, Object value, ICacheLayerMarshaller marshaller) {

        Optional result = marshaller.marshall(value);
        result.ifPresent( r -> cache.put(cacheLayerKey.apply(hashKey, rangeKey), r));
        return value;
    }

    @Override
    public Object setEx(String hashKey, String rangeKey, Object value, int expiration, ICacheLayerMarshaller marshaller) {
        return this.set(hashKey, rangeKey, value, marshaller);
    }

    @Override
    public Optional<Object> get(String hashKey, String rangeKey, ICacheLayerMarshaller marshaller)
    {
        Object result = cache.get(cacheLayerKey.apply(hashKey, rangeKey));
        return (result == null) ? Optional.empty() : marshaller.unMarshall(result);
    }

    @Override
    public boolean delete(String hashKey, String rangeKey) {
        cache.remove(cacheLayerKey.apply(hashKey, rangeKey));
        return true;
    }

    @Override
    public ICacheLayerService<CacheLayerKey> withKeyFunction(CacheLayerKey k) {
        this.cacheLayerKey = k;
        return this;
    }


}
