package com.enryold.onioncache.services;


import com.enryold.onioncache.interfaces.ICacheLayerDataModel;
import com.enryold.onioncache.interfaces.ICacheLayerMarshaller;
import com.enryold.onioncache.interfaces.ICacheLayerService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Optional;


public class OnionRedisService implements ICacheLayerService {

    private JedisPool jedisPool;
    private String host;
    private Integer port;



    public OnionRedisService()
    {}

    public OnionRedisService(String host, Integer port)
    {
        this.host = host;
        this.port = port;
        this.init();
    }

    public void init() {
        jedisPool= (jedisPool != null) ? jedisPool : new JedisPool(new JedisPoolConfig(), host, port);
    }

    @Override
    public boolean set(ICacheLayerDataModel value, ICacheLayerMarshaller marshaller) {
        try (Jedis jedis = jedisPool.getResource())
        {
            Optional<String> val = marshaller.marshall(value);
            val.ifPresent( s -> jedis.set(value.getCustomDataKey(), s));
            return true;
        }
        catch (Exception e) {  }
        return false;
    }

    @Override
    public boolean setEx(ICacheLayerDataModel value, int expiration, ICacheLayerMarshaller marshaller) {
        try (Jedis jedis = jedisPool.getResource())
        {
            Optional<String> val = marshaller.marshall(value);
            val.ifPresent( s -> jedis.setex(value.getCustomDataKey(), expiration, s));
            return true;
        }
        catch (Exception e) {  }
        return false;
    }

    @Override
    public Optional get(ICacheLayerDataModel value, ICacheLayerMarshaller marshaller) {
        try (Jedis jedis = jedisPool.getResource())
        {
            String result = jedis.get(value.getCustomDataKey());
            return result == null ? Optional.empty() : marshaller.unMarshall(result);
        }
        catch (Exception e) { return Optional.empty(); }
    }

    @Override
    public boolean delete(ICacheLayerDataModel value) {
        try (Jedis jedis = jedisPool.getResource()) { jedis.del(value.getCustomDataKey()); return true; }
        catch (Exception e) { return false; }
    }




}
