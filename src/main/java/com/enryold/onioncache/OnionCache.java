package com.enryold.onioncache;

import com.enryold.onioncache.interfaces.ICacheLayerKey;
import com.enryold.onioncache.interfaces.ICacheLayerMarshaller;
import com.enryold.onioncache.interfaces.ICacheLayerService;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Optional;

/**
 * Created by enryold on 17/01/17.
 */
public class OnionCache<T>
{
    private ArrayList<CacheLayer<? extends ICacheLayerService, ? extends ICacheLayerMarshaller, T>> layers = new ArrayList<>();




    public OnionCache addLayer(CacheLayer<? extends ICacheLayerService, ? extends ICacheLayerMarshaller, T> layer)
    {
        layers.add(layer);
        return this;
    }

    public OnionCache addLayerAtLevel(CacheLayer<? extends ICacheLayerService, ? extends ICacheLayerMarshaller, T> layer, int level)
    {
        layers.add(level, layer);
        return this;
    }


    public Optional<T> findByHashAndRange(String hash, String range)
    {
        ArrayList<CacheLayer<? extends ICacheLayerService, ? extends ICacheLayerMarshaller, T>> cacheMissLayers = new ArrayList<>();

        ListIterator<CacheLayer<?, ? extends ICacheLayerMarshaller, T>> iterator = layers.listIterator();

        Optional<T> result = Optional.empty();

        while (iterator.hasNext())
        {
            CacheLayer<?, ?, T> layer = iterator.next();
            result = layer.get(hash, range);
            if(result.isPresent()) { break; }
            else{ cacheMissLayers.add(layer); }
        }

        if(cacheMissLayers.size() > 0 && result.isPresent())
        {
            T r = result.get();
            cacheMissLayers.forEach(o -> o.set(r, hash, range));
        }

        return result;
    }

    public Optional<T> findByHash(String hash)
    {
        return this.findByHashAndRange(hash, null);
    }


    public boolean set(String hash, String range, T object, int expire)
    {
        for (CacheLayer<?, ?, T> layer : layers) {
            if (!layer.set(object, hash, range, expire)) {
                return false;
            }
        }
        return true;

    }

    public boolean set(String hash, T object, int expire)
    {
        for (CacheLayer<?, ?, T> layer : layers) {
            if (!layer.set(object, hash, expire)) {
                return false;
            }
        }
        return true;

    }

    public boolean set(String hash, String range, T object)
    {
        for (CacheLayer<?, ?, T> layer : layers) {
            if (!layer.set(object, hash, range)) {
                return false;
            }
        }
        return true;
    }

    public boolean set(String hash, T object)
    {
        for (CacheLayer<?, ?, T> layer : layers) {
            if (!layer.set(object, hash)) {
                return false;
            }
        }
        return true;
    }
}
