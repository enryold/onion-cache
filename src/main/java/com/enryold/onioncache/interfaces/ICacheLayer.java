package com.enryold.onioncache.interfaces;

import java.util.Optional;

/**
 * Created by enryold on 16/01/17.
 */
public interface ICacheLayer<S extends ICacheLayerService, M extends ICacheLayerMarshaller<T, ?>, T>
{
    ICacheLayer<S, M, T> withMainService(S service);
    ICacheLayer<S, M, T> withMainServiceMarshaller(M marshaller);
    ICacheLayer<S, M, T> withDefaultExpiration(int defaultExpiration);

    boolean set(T t, String hashKey, String rangeKey, int expire);
    boolean set(T t, String hashKey, int expire);

    boolean set(T t, String hashKey);
    boolean set(T t, String hashKey, String rangeKey);

    Optional<T> get(String hashKey);
    Optional<T> get(String hashKey, String rangeKey);

    boolean delete(String hashKey);
    boolean delete(String hashKey, String rangeKey);


}
