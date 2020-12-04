package de.derteufelqwe.bungeeplugin.redis;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.events.RedisPlayerAddEvent;
import de.derteufelqwe.bungeeplugin.redis.events.RedisPlayerRemoveEvent;
import de.derteufelqwe.bungeeplugin.redis.events.RedisPlayerServerChangeEvent;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerConnectMessage;
import de.derteufelqwe.bungeeplugin.utils.Utils;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

/**
 * Receives the published messages on redis and dispatches them.
 * This class should run in a different thread since the {@link #run()} method is blocking
 */
public class RedisPublishListener extends JedisPubSub implements Runnable {

    private JedisPool jedisPool;
    private String bungeeCordId;
    private RedisDataCache redisDataCache;


    public RedisPublishListener(JedisPool jedisPool, RedisDataCache redisDataCache) {
        this.bungeeCordId = BungeePlugin.META_DATA.getTaskName();
        this.jedisPool = jedisPool;
        this.redisDataCache = redisDataCache;
    }


    @Override
    public void run() {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.psubscribe(this, "events#*", "messages#*");
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

        if (pattern.equals("events#*")) {
            String event = channel.substring(7);
            this.onEventMessage(event, message);

        } else if (pattern.equals("messages#*")) {
            String msg = channel.substring(9);
            this.onMessageMessage(msg, message);
        }

    }

    private void onEventMessage(String event, String data) {
        RedisPubSubData redisEvent = null;

        switch (event) {
            case "playerJoin":
                redisEvent = RedisPubSubData.deserialize(data, RedisPlayerAddEvent.class);
                if (this.checkEventNotFromHere(redisEvent)) {
                    this.onPlayerAddEvent((RedisPlayerAddEvent) redisEvent);
                }
                break;

            case "playerLeave":
                redisEvent = RedisPubSubData.deserialize(data, RedisPlayerRemoveEvent.class);
                if (this.checkEventNotFromHere(redisEvent)) {
                    this.onPlayerRemoveEvent((RedisPlayerRemoveEvent) redisEvent);
                }
                break;

            case "playerServerChange":
                redisEvent = RedisPubSubData.deserialize(data, RedisPlayerServerChangeEvent.class);
                if (this.checkEventNotFromHere(redisEvent)) {
                    this.onPlayerServerChangeEvent((RedisPlayerServerChangeEvent) redisEvent);
                }
                break;

            default:
                System.err.println("Found unknown event '" + event + "'.");
                break;
        }
    }

    private void onMessageMessage(String message, String data) {
        RedisPubSubData redisMsg;

        switch (message) {
            case "connectPlayer":
                redisMsg = RedisPubSubData.deserialize(data, RedisPlayerConnectMessage.class);
                if (this.checkEventNotFromHere(redisMsg)) {
                    this.onConnectPlayerMessage((RedisPlayerConnectMessage) redisMsg);
                }
                break;

            default:
                System.err.printf("Found unknown message %s.\n", message);
                break;

        }
    }


    /**
     * Returns false if the event was fired from this host.
     * @return
     */
    private boolean checkEventNotFromHere(RedisPubSubData event) {
        return !event.getBungeeCordId().equals(BungeePlugin.META_DATA.getTaskName());
    }

    // -----  Event handlers  -----

    private void onPlayerAddEvent(RedisPlayerAddEvent event) {
        System.out.printf("Event: PlayerAdd %s.\n", event.getUsername());
        this.redisDataCache.loadPlayerFromRedis(event.getUsername());
    }

    private void onPlayerRemoveEvent(RedisPlayerRemoveEvent event) {
        System.out.printf("Event: PlayerRemove %s.\n", event.getUsername());
        this.redisDataCache.removePlayerFromCache(event.getUsername());
    }

    private void onPlayerServerChangeEvent(RedisPlayerServerChangeEvent event) {
        System.out.printf("Event: PlayerChange %s.\n", event.getUsername());
        this.redisDataCache.loadPlayerFromRedis(event.getUsername());
    }

    // -----  Message handlers  -----

    private void onConnectPlayerMessage(RedisPlayerConnectMessage message) {
        // Only execute this, if the message is intended for this bungeecord
        if (!message.getTargetBungee().equals(this.bungeeCordId)) {
            return;
        }

        ServerInfo serverInfo = Utils.getServers().get(message.getTargetServer());
        if (serverInfo == null) {
            System.err.printf("Trying to connect to invalid server %s.\n", message.getTargetServer());
            return;
        }

        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(message.getUsername());
        if (player == null) {
            System.err.printf("Couldn't find user %s to connect to %s.\n", message.getUsername(), message.getTargetServer());
            return;
        }

        player.connect(serverInfo, ServerConnectEvent.Reason.PLUGIN);
    }

}

