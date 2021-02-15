package de.derteufelqwe.bungeeplugin.redis;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerJoinEvent;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerLeaveEvent;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerServerChangeEvent;
import de.derteufelqwe.bungeeplugin.events.BungeeRequestPlayerServerSendEvent;
import de.derteufelqwe.bungeeplugin.redis.messages.*;
import de.derteufelqwe.bungeeplugin.runnables.DefaultCallback;
import de.derteufelqwe.commons.exceptions.NotFoundException;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

/**
 * Receives the published messages on redis and dispatches them into BungeeCord events
 * This class should run in a different thread since the {@link #run()} method is blocking
 */
public class RedisPublishListener extends JedisPubSub implements Runnable {

    private JedisPool jedisPool = BungeePlugin.getRedisPool().getJedisPool();
    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();


    public RedisPublishListener() {

    }


    @Override
    public void run() {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.psubscribe(this, "messages#*");
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

        if (pattern.equals("messages#*")) {
            String msg = channel.substring(9);
            try {
                MessageType messageType = MessageType.valueOf(msg);
                this.onMessageMessage(messageType, message);

            } catch (IllegalArgumentException e) {
                System.err.println("Received invalid message type " + msg + ".");
            }

        } else {
            System.err.println("Received unknown Pub-Sub pattern '" + pattern + "'.");
        }

    }

    private void onMessageMessage(MessageType type, String data) {
        RedisPubSubData redisMessage;

        switch (type) {
            case PLAYER_JOIN:
                redisMessage = RedisPubSubData.deserialize(data, RedisPlayerJoinNetwork.class);
                if (this.checkEventNotFromHere(redisMessage)) {
                    this.onPlayerAddMessage((RedisPlayerJoinNetwork) redisMessage);
                }
                break;

            case PLAYER_LEAVE:
                redisMessage = RedisPubSubData.deserialize(data, RedisPlayerLeaveNetwork.class);
                if (this.checkEventNotFromHere(redisMessage)) {
                    this.onPlayerRemoveMessage((RedisPlayerLeaveNetwork) redisMessage);
                }
                break;

            case PLAYER_SERVER_CHANGE:
                redisMessage = RedisPubSubData.deserialize(data, RedisPlayerServerChange.class);
                if (this.checkEventNotFromHere(redisMessage)) {
                    this.onPlayerServerChangeMessage((RedisPlayerServerChange) redisMessage);
                }
                break;

            case REQUEST_PLAYER_SERVER_CHANGE:
                redisMessage = RedisPubSubData.deserialize(data, RedisRequestPlayerServerSend.class);
                if (this.checkEventNotFromHere(redisMessage)) {
                    this.onConnectPlayerMessage((RedisRequestPlayerServerSend) redisMessage);
                }
                break;

            case REQUEST_PLAYER_KICK:
                redisMessage = RedisPubSubData.deserialize(data, RedisRequestPlayerKick.class);
                if (this.checkEventNotFromHere(redisMessage)) {
                    this.onKickPlayerMessage((RedisRequestPlayerKick) redisMessage);
                }
                break;


            default:
                System.err.printf("Found unknown message %s.\n", type);
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

    // -----  Message handlers  -----

    private void onPlayerAddMessage(RedisPlayerJoinNetwork message) {
        System.out.printf("Event: PlayerAdd %s.\n", message.getUsername());
        ProxyServer.getInstance().getPluginManager().callEvent(new BungeePlayerJoinEvent(message.getUuid(), message.getUsername(), new Callback<BungeePlayerJoinEvent>() {
            @Override
            public void done(BungeePlayerJoinEvent result, Throwable error) {

            }
        }));
    }

    private void onPlayerRemoveMessage(RedisPlayerLeaveNetwork message) {
        System.out.printf("Event: PlayerRemove %s.\n", message.getUsername());

        ProxyServer.getInstance().getPluginManager().callEvent(new BungeePlayerLeaveEvent(message.getUuid(), message.getUsername(), new DefaultCallback<>()));
    }

    private void onPlayerServerChangeMessage(RedisPlayerServerChange message) {
        System.out.printf("Event: PlayerChange %s.\n", message.getUsername());

        ProxyServer.getInstance().getPluginManager().callEvent(new BungeePlayerServerChangeEvent(message, new Callback<BungeePlayerServerChangeEvent>() {
            @Override
            public void done(BungeePlayerServerChangeEvent result, Throwable error) {

            }
        }));
    }

    private void onConnectPlayerMessage(RedisRequestPlayerServerSend message) {
        System.out.printf("Event: RequestPlayerSend %s -> %s.\n", message.getUsername(), message.getTargetServer());
        // You can only send players that are connected on your proxy
        if (!message.getTargetBungee().equals(BungeePlugin.BUNGEECORD_ID)) {
            return;
        }

        ProxyServer.getInstance().getPluginManager().callEvent(new BungeeRequestPlayerServerSendEvent(message, new Callback<BungeeRequestPlayerServerSendEvent>() {
            @Override
            public void done(BungeeRequestPlayerServerSendEvent result, Throwable error) {

            }
        }));
    }

    private void onKickPlayerMessage(RedisRequestPlayerKick message) {
        try {
            try {
                UUID uuid = UUID.fromString(message.getUsername());
                BungeePlugin.getBungeeApi().kickPlayer(uuid, message.getReason());

            } catch (IllegalArgumentException e) {
                BungeePlugin.getBungeeApi().kickPlayer(message.getUsername(), message.getReason());
            }

        } catch (NotFoundException e) {

        }
    }

}


