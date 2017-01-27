package com.enryold.onioncache;

import com.enryold.onioncache.interfaces.ICacheLayerDataModel;
import com.enryold.onioncache.interfaces.ICacheLayerMarshaller;
import com.enryold.onioncache.interfaces.ICacheLayerService;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Optional;

/**
 * Created by enryold on 17/01/17.
 */
public class OnionCache<T extends ICacheLayerDataModel>
{
    private ArrayList<OnionCacheLayer<? extends ICacheLayerService, ? extends ICacheLayerMarshaller, T>> layers = new ArrayList<>();




    public OnionCache addLayer(OnionCacheLayer<? extends ICacheLayerService, ? extends ICacheLayerMarshaller, T> layer)
    {
        layers.add(layer);
        return this;
    }

    public OnionCache addLayerAtLevel(OnionCacheLayer<? extends ICacheLayerService, ? extends ICacheLayerMarshaller, T> layer, int level)
    {
        layers.add(level, layer);
        return this;
    }


    public Optional<T> get(T t)
    {
        ArrayList<OnionCacheLayer<? extends ICacheLayerService, ? extends ICacheLayerMarshaller, T>> cacheMissLayers = new ArrayList<>();

        ListIterator<OnionCacheLayer<?, ? extends ICacheLayerMarshaller, T>> iterator = layers.listIterator();

        Optional<T> result = Optional.empty();

        while (iterator.hasNext())
        {
            OnionCacheLayer<?, ?, T> layer = iterator.next();
            result = layer.get(t);
            if(result.isPresent()) { break; }
            else{ cacheMissLayers.add(layer); }
        }

        if(cacheMissLayers.size() > 0 && result.isPresent())
        {
            T r = result.get();
            cacheMissLayers.forEach(o -> o.set(r));
        }

        return result;
    }



    public boolean set(T object, int expire)
    {
        for (OnionCacheLayer<?, ?, T> layer : layers) {
            if (!layer.set(object, expire)) {
                return false;
            }
        }
        return true;

    }


    public boolean delete(T object)
    {
        for (OnionCacheLayer<?, ?, T> layer : layers) {
            if (!layer.delete(object)) {
                return false;
            }
        }
        return true;

    }

}
