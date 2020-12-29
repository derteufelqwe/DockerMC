package de.derteufelqwe.bungeeplugin.redis;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerJoinNetwork;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerLeaveNetwork;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerServerChange;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisRequestPlayerServerSend;
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

    private JedisPool jedisPool = BungeePlugin.getRedisHandler().getJedisPool();
    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();


    public RedisPublishListener() {

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
                redisEvent = RedisPubSubData.deserialize(data, RedisPlayerJoinNetwork.class);
                if (this.checkEventNotFromHere(redisEvent)) {
                    this.onPlayerAddEvent((RedisPlayerJoinNetwork) redisEvent);
                }
                break;

            case "playerLeave":
                redisEvent = RedisPubSubData.deserialize(data, RedisPlayerLeaveNetwork.class);
                if (this.checkEventNotFromHere(redisEvent)) {
                    this.onPlayerRemoveEvent((RedisPlayerLeaveNetwork) redisEvent);
                }
                break;

            case "playerServerChange":
                redisEvent = RedisPubSubData.deserialize(data, RedisPlayerServerChange.class);
                if (this.checkEventNotFromHere(redisEvent)) {
                    this.onPlayerServerChangeEvent((RedisPlayerServerChange) redisEvent);
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
                redisMsg = RedisPubSubData.deserialize(data, RedisRequestPlayerServerSend.class);
                if (this.checkEventNotFromHere(redisMsg)) {
                    this.onConnectPlayerMessage((RedisRequestPlayerServerSend) redisMsg);
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

    private void onPlayerAddEvent(RedisPlayerJoinNetwork event) {
        System.out.printf("Event: PlayerAdd %s.\n", event.getUsername());
        this.redisDataManager.loadPlayer(event.getUsername());
    }

    private void onPlayerRemoveEvent(RedisPlayerLeaveNetwork event) {
        System.out.printf("Event: PlayerRemove %s.\n", event.getUsername());
        this.redisDataManager.removePlayerLoc(event.getUsername());
    }

    private void onPlayerServerChangeEvent(RedisPlayerServerChange event) {
        System.out.printf("Event: PlayerChange %s.\n", event.getUsername());
        this.redisDataManager.loadPlayer(event.getUsername());
    }

    // -----  Message handlers  -----

    private void onConnectPlayerMessage(RedisRequestPlayerServerSend message) {
        // Only execute this, if the message is intended for this bungeecord
        if (!message.getTargetBungee().equals(BungeePlugin.BUNGEECORD_ID)) {
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

