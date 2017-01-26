package com.enryold.onioncache.services;


import com.enryold.onioncache.interfaces.ICacheLayerDataModel;
import com.enryold.onioncache.interfaces.ICacheLayerMarshaller;
import com.enryold.onioncache.interfaces.ICacheLayerService;
import com.enryold.onioncache.utils.LRUMap;

import java.util.Optional;


public class InMemoryLRUService implements ICacheLayerService {

    private LRUMap<String, Object> cache;




    public InMemoryLRUService(int capacity)
    {
        cache = new LRUMap<>(capacity);
    }


    @Override
    public boolean set(ICacheLayerDataModel value, ICacheLayerMarshaller marshaller) {
        Optional m = marshaller.marshall(value);
        cache.put(value.dataModelUniqueKey().get(), m.get());
        return m.isPresent();
    }

    @Override
    public boolean setEx(ICacheLayerDataModel value, int expiration, ICacheLayerMarshaller marshaller) {
        return this.set(value, marshaller);
    }

    @Override
    public Optional get(ICacheLayerDataModel value, ICacheLayerMarshaller marshaller) {
        return marshaller.unMarshall(cache.get(value.dataModelUniqueKey().get()));
    }

    @Override
    public boolean delete(ICacheLayerDataModel value) {
        return cache.remove(value.dataModelUniqueKey().get()) != null;
    }
}
