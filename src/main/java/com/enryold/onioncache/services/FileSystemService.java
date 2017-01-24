package com.enryold.onioncache.services;

import com.enryold.onioncache.CacheLayerKey;
import com.enryold.onioncache.interfaces.ICacheLayerMarshaller;
import com.enryold.onioncache.interfaces.ICacheLayerService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Created by enryold on 18/01/17.
 */
public class FileSystemService implements ICacheLayerService<CacheLayerKey> {


    private String basePath;
    private CacheLayerKey cacheLayerKey;


    public FileSystemService(String basePath) throws Exception {

        if(!Files.isWritable(Paths.get(basePath)))
        {
            throw new Exception("The path "+basePath+" is not writable");
        }
        else
        {
            this.basePath = basePath;
        }

    }



    private Path pathFromKey(String key)
    {
        return Paths.get(basePath+File.separator+key);
    }

    private byte[] byteFromValue(Object value)
    {
        try {
            if(value instanceof String)
            {
                String v = (String)value;
                return v.getBytes("utf-8");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }


    @Override
    public ICacheLayerService<CacheLayerKey> withKeyFunction(CacheLayerKey k) {
        this.cacheLayerKey = k;
        return this;
    }



    @Override
    public Object set(String hashKey, String rangeKey, Object value, ICacheLayerMarshaller marshaller) {

        try
        {
            String key = cacheLayerKey.apply(hashKey, rangeKey);
            Optional result = marshaller.marshall(value);

            if(!result.isPresent()) { return null; }

            Files.write(pathFromKey(key), byteFromValue(result.get()));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }


        return value;
    }

    @Override
    public Object setEx(String hashKey, String rangeKey, Object value, int expiration, ICacheLayerMarshaller marshaller)
    {
        return this.set(hashKey, rangeKey, value, marshaller);
    }

    @Override
    public Optional<Object> get(String hashKey, String rangeKey, ICacheLayerMarshaller marshaller) {
        try
        {
            String key = cacheLayerKey.apply(hashKey, rangeKey);
            return marshaller.unMarshall(new String(Files.readAllBytes(pathFromKey(key))));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String hashKey, String rangeKey) {

        try {
            Files.delete(pathFromKey(cacheLayerKey.apply(hashKey, rangeKey)));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
