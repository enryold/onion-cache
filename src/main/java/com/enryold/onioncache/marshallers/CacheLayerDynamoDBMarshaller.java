package com.enryold.onioncache.marshallers;

import com.enryold.onioncache.CacheLayerMarshaller;
import com.enryold.onioncache.interfaces.ICacheLayerMarshaller;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by enryold on 19/01/17.
 */
public class CacheLayerDynamoDBMarshaller<I> extends CacheLayerMarshaller implements ICacheLayerMarshaller<I, I> {

    /** THIS IS A FAKE MARSHALLER CAUSE DYNAMO-DB ALREADY HAVE HIS MARSHALLER */

    public CacheLayerDynamoDBMarshaller(Class iClazz, Class oClazz) {
        super(iClazz, oClazz);
    }

    @Override
    public Optional<I> marshall(I i) {
        return Optional.of(i);
    }

    @Override
    public Optional<I> unMarshall(I o) {
        return Optional.of(o);
    }
}
