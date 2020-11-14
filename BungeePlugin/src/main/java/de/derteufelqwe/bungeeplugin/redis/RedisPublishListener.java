package de.derteufelqwe.bungeeplugin.redis;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.events.RedisPlayerAddEvent;
import de.derteufelqwe.bungeeplugin.redis.events.RedisPlayerRemoveEvent;
import de.derteufelqwe.bungeeplugin.redis.events.RedisEvent;
import de.derteufelqwe.bungeeplugin.redis.events.RedisPlayerServerChangeEvent;
import net.md_5.bungee.api.ProxyServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

public class RedisPublishListener extends JedisPubSub implements Runnable {

    private JedisPool jedisPool;
    private String bungeeCordId;
    private RedisDataCache redisDataCache;


    public RedisPublishListener(JedisPool jedisPool, RedisDataCache redisDataCache) {
        this.bungeeCordId = BungeePlugin.META_DATA.getTaskName();
        this.jedisPool = jedisPool;
        this.redisDataCache = redisDataCache;
    }


    public void init() {
        System.out.println("Starting publish thread");
        ProxyServer.getInstance().getScheduler().runAsync(BungeePlugin.PLUGIN, this);
    }


    @Override
    public void run() {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.psubscribe(this, "events#*");
        }
    }

    /**
     * Dispatches the incoming redis publish messages.
     * @param pattern
     * @param channel
     * @param message
     */
    @Override
    public void onPMessage(String pattern, String channel, String message) {
        String event = channel.substring(7);

        RedisEvent redisEvent = null;
        switch (event) {
            case "playerJoin":
                redisEvent = RedisEvent.deserialize(message, RedisPlayerAddEvent.class);
                if (this.checkEventNotFromHere(redisEvent)) {
                    this.onPlayerAdd((RedisPlayerAddEvent) redisEvent);
                }
                break;

            case "playerLeave":
                redisEvent = RedisEvent.deserialize(message, RedisPlayerRemoveEvent.class);
                if (this.checkEventNotFromHere(redisEvent)) {
                    this.onPlayerRemove((RedisPlayerRemoveEvent) redisEvent);
                }
                break;

            case "playerServerChange":
                redisEvent = RedisEvent.deserialize(message, RedisPlayerServerChangeEvent.class);
                if (this.checkEventNotFromHere(redisEvent)) {
                    this.onPlayerServerChange((RedisPlayerServerChangeEvent) redisEvent);
                }
                break;

            default:
                System.err.println("Found unknown event '" + event + "'.");
                break;
        }

    }

    /**
     * Returns false if the event was fired from this host.
     * @return
     */
    private boolean checkEventNotFromHere(RedisEvent event) {
        return !event.getBungeeCordId().equals(BungeePlugin.META_DATA.getTaskName());
    }


    private void onPlayerAdd(RedisPlayerAddEvent event) {
        System.out.printf("Event: PlayerAdd %s.\n", event.getUsername());
        this.redisDataCache.loadPlayerFromRedis(event.getUsername());
    }

    private void onPlayerRemove(RedisPlayerRemoveEvent event) {
        System.out.printf("Event: PlayerRemove %s.\n", event.getUsername());
        this.redisDataCache.removePlayerFromCache(event.getUsername());
    }

    private void onPlayerServerChange(RedisPlayerServerChangeEvent event) {
        System.out.printf("Event: PlayerChange %s.\n", event.getUsername());
        this.redisDataCache.loadPlayerFromRedis(event.getUsername());
    }

}

