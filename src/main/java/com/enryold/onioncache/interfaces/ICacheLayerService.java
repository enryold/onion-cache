package com.enryold.onioncache.interfaces;

import java.util.Optional;

/**
 * Created by enryold on 16/01/17.
 */
public interface ICacheLayerService<T extends ICacheLayerDataModel>
{
    boolean set(T value, ICacheLayerMarshaller marshaller);
    boolean setEx(T value, int expiration, ICacheLayerMarshaller marshaller);
    Optional<T> get(T value, ICacheLayerMarshaller marshaller);
    boolean delete(T value);
}
