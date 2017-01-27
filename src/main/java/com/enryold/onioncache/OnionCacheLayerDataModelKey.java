package com.enryold.onioncache;

import com.enryold.onioncache.interfaces.ICacheLayerDataModelKey;

/**
 * Created by enryold on 26/01/17.
 */
public class OnionCacheLayerDataModelKey implements ICacheLayerDataModelKey {

    String key;

    public OnionCacheLayerDataModelKey(String key)
    {
        this.key = key;
    }

    @Override
    public String get() {
        return key;
    }
}
