package com.enryold.onioncache.interfaces;

import java.util.function.BiFunction;

/**
 * Created by enryold on 16/01/17.
 */
public interface ICacheLayerKey extends BiFunction<String, String, String>
{
    String PREFIX = "cache_layer";
    String SEPARATOR = "_";
}
