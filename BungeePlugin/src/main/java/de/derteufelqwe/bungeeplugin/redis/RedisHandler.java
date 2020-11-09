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

        config.setMaxTotal(64);
        config.setMaxIdle(64);
        config.setMinIdle(8);
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
