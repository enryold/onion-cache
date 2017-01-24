package com.enryold.onioncache.interfaces;

import java.util.Optional;

/**
 * Created by enryold on 16/01/17.
 */
public interface ICacheLayerService<KeyFunction extends ICacheLayerKey>
{
    ICacheLayerService<KeyFunction> withKeyFunction(KeyFunction k);

    Object set(String hashKey, String rangeKey, Object value, ICacheLayerMarshaller marshaller);
    Object setEx(String hashKey, String rangeKey, Object value, int expiration, ICacheLayerMarshaller marshaller);
    Optional<Object> get(String hashKey, String rangeKey, ICacheLayerMarshaller marshaller);
    boolean delete(String hashKey, String rangeKey);
}
