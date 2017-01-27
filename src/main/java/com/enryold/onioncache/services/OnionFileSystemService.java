package com.enryold.onioncache.services;

import com.enryold.onioncache.interfaces.ICacheLayerDataModel;
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
public class OnionFileSystemService implements ICacheLayerService {


    private String basePath;


    public OnionFileSystemService(String basePath) throws Exception {

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
    public boolean set(ICacheLayerDataModel value, ICacheLayerMarshaller marshaller) {
        try
        {
            Optional result = marshaller.marshall(value);

            if(!result.isPresent()) { return false; }

            Files.write(pathFromKey(value.dataModelUniqueKey().get()), byteFromValue(result.get()));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return false;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }


        return true;
    }

    @Override
    public boolean setEx(ICacheLayerDataModel value, int expiration, ICacheLayerMarshaller marshaller) {
        return this.set(value, marshaller);
    }

    @Override
    public Optional get(ICacheLayerDataModel value, ICacheLayerMarshaller marshaller) {
        try
        {
            return marshaller.unMarshall(new String(Files.readAllBytes(pathFromKey(value.dataModelUniqueKey().get()))));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(ICacheLayerDataModel value) {
        try {
            Files.delete(pathFromKey(value.dataModelUniqueKey().get()));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
