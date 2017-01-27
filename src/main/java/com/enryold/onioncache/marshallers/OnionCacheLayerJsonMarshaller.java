package com.enryold.onioncache.marshallers;

import com.enryold.onioncache.OnionCacheLayerMarshaller;
import com.enryold.onioncache.interfaces.ICacheLayerMarshaller;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by enryold on 19/01/17.
 */
public class OnionCacheLayerJsonMarshaller<I> extends OnionCacheLayerMarshaller<I, String> implements ICacheLayerMarshaller<I, String> {


    public OnionCacheLayerJsonMarshaller(Class<I> iClazz, Class<String> oClazz) {
        super(iClazz, oClazz);
    }

    @Override
    public Optional<String> marshall(I t)
    {
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            return Optional.of(mapper.writeValueAsString(t));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<I> unMarshall(String json)
    {
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            return Optional.of(mapper.readValue(json, getInputClazz()));
        }
        catch (JsonProcessingException e) {
            return Optional.empty();
        }
        catch (IOException e) {
            return Optional.empty();
        }
    }
}
