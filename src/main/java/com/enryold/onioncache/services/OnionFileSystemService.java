package com.enryold.onioncache.services;

import com.enryold.onioncache.interfaces.ICacheLayerDataModel;
import com.enryold.onioncache.interfaces.ICacheLayerMarshaller;
import com.enryold.onioncache.interfaces.ICacheLayerService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by enryold on 18/01/17.
 */
public class OnionFileSystemService implements ICacheLayerService {


    private String basePath;
    private boolean withGzip;


    public OnionFileSystemService(String basePath) throws Exception {

        if(!Files.isWritable(Paths.get(basePath)))
        {
            throw new Exception("The path "+basePath+" is not writable");
        }
        else
        {
            this.basePath = basePath;
            this.withGzip = false;
        }

    }

    public OnionFileSystemService withGzipCompression(boolean withGzip)
    {
        this.withGzip = true;
        return this;
    }


    private String ungzip(byte[] bytes)
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(bytes)), StandardCharsets.UTF_8);
            StringWriter sw = new StringWriter();
            char[] chars = new char[1024];
            for (int len; (len = isr.read(chars)) > 0; ) {
                sw.write(chars, 0, len);
            }
            String result = sw.toString();
            sw.close();
            isr.close();
            return result;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }

    }

    private byte[] gzip(Object s)
    {
        try
        {
            if(s instanceof String)
            {
                String v = (String)s;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                GZIPOutputStream gzip = new GZIPOutputStream(bos);
                OutputStreamWriter osw = new OutputStreamWriter(gzip, StandardCharsets.UTF_8);
                osw.write(v);
                osw.close();
                byte[] result =  bos.toByteArray();
                bos.close();
                gzip.close();
                return result;
            }
            else
            {
                return new byte[0];
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new byte[0];
        }

    }


    public Path pathForObject(ICacheLayerDataModel value)
    {
        return pathFromKey(value.getCustomDataKey());
    }


    private Path pathFromKey(String key)
    {
        return Paths.get(basePath+File.separator+key+((withGzip) ? ".gz" : ""));
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

            byte[] bytes = (this.withGzip) ? gzip(result.get()) : byteFromValue(result.get());

            Files.write(pathFromKey(value.getCustomDataKey()), bytes);
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
            Path path = pathFromKey(value.getCustomDataKey());

            if(!Files.exists(path)) { return Optional.empty(); }

            byte[] bytes = Files.readAllBytes(path);

            String result = (this.withGzip) ? ungzip(bytes) : new String(bytes);

            if(result == null) { return Optional.empty(); }

            return marshaller.unMarshall(result);
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
            Path path = pathFromKey(value.getCustomDataKey());
            if(!Files.exists(path)) { return false; }

            Files.delete(pathFromKey(value.getCustomDataKey()));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
