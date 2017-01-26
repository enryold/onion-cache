package com.enryold.onioncache.interfaces;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Created by enryold on 16/01/17.
 */
public interface ICacheLayerMarshaller<I, O>
{
    Optional<O> marshall(I i);
    Optional<I> unMarshall(O o);
}
