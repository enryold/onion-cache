package com.enryold.onioncache.interfaces;

import java.util.Optional;

/**
 * Created by enryold on 16/01/17.
 */
public interface ICacheLayer<S extends ICacheLayerService, M extends ICacheLayerMarshaller<T, ?>, T extends ICacheLayerDataModel>
{
    ICacheLayer<S, M, T> withMainService(S service);
    ICacheLayer<S, M, T> withMainServiceMarshaller(M marshaller);
    ICacheLayer<S, M, T> withDefaultExpiration(int defaultExpiration);


    // ORM METHODS
    boolean set(T t);
    boolean set(T t, int expire);
    Optional<T> get(T t);
    boolean delete(T t);



}
