package com.enryold.onioncache;


import com.enryold.onioncache.interfaces.ICacheLayerKey;

import java.util.Optional;

/**
 * Created by enryold on 16/01/17.
 */
public class CacheLayerKey implements ICacheLayerKey {

    @Override
    public String apply(String s, String s2) {
        return PREFIX+SEPARATOR+s+SEPARATOR+Optional.ofNullable(s2).orElse("");
    }
}
