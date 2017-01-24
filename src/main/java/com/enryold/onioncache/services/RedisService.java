package com.enryold.onioncache.services;


import com.enryold.onioncache.CacheLayerKey;
import com.enryold.onioncache.interfaces.ICacheLayerMarshaller;
import com.enryold.onioncache.interfaces.ICacheLayerService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class RedisService implements ICacheLayerService<CacheLayerKey> {

    private JedisPool jedisPool;
    private String host;
    private Integer port;
    private CacheLayerKey cacheLayerKey;



    public RedisService()
    {}

    public RedisService(String host, Integer port)
    {
        this.host = host;
        this.port = port;
        this.init();
    }

    public void init() {
        jedisPool= (jedisPool != null) ? jedisPool : new JedisPool(new JedisPoolConfig(), host, port);
    }

    @Override
    public Optional<Object> get(String hashKey, String rangeKey, ICacheLayerMarshaller marshaller) {
        try (Jedis jedis = jedisPool.getResource())
        {
            String result = jedis.get(cacheLayerKey.apply(hashKey, rangeKey));
            return result == null ? Optional.empty() : marshaller.unMarshall(result);
        }
        catch (Exception e) { return Optional.empty(); }
    }

    @Override
    public boolean delete(String hashKey, String rangeKey) {
        try (Jedis jedis = jedisPool.getResource()) { jedis.del(cacheLayerKey.apply(hashKey, rangeKey)); return true; }
        catch (Exception e) { return false; }
    }

    @Override
    public Object set(String hashKey, String rangeKey, Object value, ICacheLayerMarshaller marshaller) {
        try (Jedis jedis = jedisPool.getResource())
        {
            if(marshaller.getOutputClazz().equals(String.class))
            {
                Optional<String> val = marshaller.marshall(value);
                val.ifPresent( s -> jedis.set(cacheLayerKey.apply(hashKey, rangeKey), s));
                return value;
            }
        }
        catch (Exception e) {  }
        return null;
    }

    @Override
    public Object setEx(String hashKey, String rangeKey, Object value, int secondsExpire, ICacheLayerMarshaller marshaller) {
        try (Jedis jedis = jedisPool.getResource())
        {
            if(marshaller.getOutputClazz().equals(String.class))
            {
                Optional<String> val = marshaller.marshall(value);
                val.ifPresent( s -> jedis.setex(cacheLayerKey.apply(hashKey, rangeKey), secondsExpire, s));
                return value;
            }
        }
        catch (Exception e) {  }
        return null;
    }


    @Override
    public ICacheLayerService<CacheLayerKey> withKeyFunction(CacheLayerKey k) {
        this.cacheLayerKey = k;
        return this;
    }


}
