package de.derteufelqwe.bungeeplugin.redis;

import lombok.Getter;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Namespaces
 * -
 */

public class RedisHandler {

    @Getter
    private JedisPool jedisPool;


    public RedisHandler(String host, int port) {
        this.jedisPool = new JedisPool(this.getJedisPoolConfig(), host, port);
    }

    public RedisHandler(String host) {
        this(host, 6379);
    }

    private JedisPoolConfig getJedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();

        config.setMaxTotal(256);
        config.setMaxIdle(128);
        config.setMinIdle(64);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);

//        config.setBlockWhenExhausted(true);

        return config;
    }

    public void destroy() {
        this.jedisPool.destroy();
    }


}
