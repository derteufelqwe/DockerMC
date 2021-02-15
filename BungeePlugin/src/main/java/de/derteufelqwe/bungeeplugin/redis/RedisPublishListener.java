package de.derteufelqwe.bungeeplugin.redis;

import com.google.protobuf.InvalidProtocolBufferException;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerJoinEvent;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerLeaveEvent;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerServerChangeEvent;
import de.derteufelqwe.bungeeplugin.events.BungeeRequestPlayerServerSendEvent;
import de.derteufelqwe.bungeeplugin.runnables.DefaultCallback;
import de.derteufelqwe.commons.protobuf.RedisMessages;
import net.md_5.bungee.api.ProxyServer;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.CheckForNull;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * Receives the published messages on redis and dispatches them into BungeeCord events
 * This class should run in a different thread since the {@link #run()} method is blocking
 */
public class RedisPublishListener extends BinaryJedisPubSub implements Runnable {

    private JedisPool jedisPool = BungeePlugin.getRedisPool().getJedisPool();
    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();


    public RedisPublishListener() {

    }


    @Override
    public void run() {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.subscribe(this, "messages".getBytes(StandardCharsets.UTF_8));
        }
    }


    @Override
    public void onMessage(byte[] channel, byte[] message) {
        try {
            RedisMessages.RedisMessage msg = RedisMessages.RedisMessage.parseFrom(message);

            switch (msg.getType()) {
                case PLAYER_JOIN_NETWORK:
                    this.onPlayerJoinMessage(msg.getPlayerJoinNetwork());
                    break;

                case PLAYER_LEAVE_NETWORK:
                    this.onPlayerLeaveMessage(msg.getPlayerLeaveNetwork());
                    break;

                case PLAYER_CHANGE_SERVER:
                    this.onPlayerServerChangeMessage(msg.getPlayerChangeServer());
                    break;

                case REQUEST_PLAYER_KICK:
                    this.onKickPlayerMessage(msg.getRequestPlayerKick());
                    break;

                case REQUEST_PLAYER_SEND:
                    this.onSendPlayerMessage(msg.getRequestPlayerSend());
                    break;

                default:
                    System.err.println("Found invalid packet type " + msg.getType());
            }

        } catch (InvalidProtocolBufferException e) {
            System.err.println("Failed to decode redis message: " + Arrays.toString(message));
        }
    }


    /**
     * Returns true if the event was fired from this host.
     * @return
     */
    private boolean checkEventNotFromHere(RedisMessages.BungeeMessageBase message) {
        return message.getBungeeCordId().equals(BungeePlugin.META_DATA.getTaskName());
    }

    @CheckForNull
    private UUID parseUUID(String uuid) {
        try {
            return UUID.fromString(uuid);

        } catch (IllegalArgumentException e) {
            System.err.println("Got invalid uuid " + uuid);
            return null;
        }
    }

    // -----  Message handlers  -----

    private void onPlayerJoinMessage(RedisMessages.PlayerJoinNetwork message) {
        if (checkEventNotFromHere(message.getBase()))
            return;
        System.out.printf("Event: PlayerAdd %s.\n", message.getUsername());

        UUID uuid = parseUUID(message.getUuid().getData());
        if (uuid == null)
            return;

        ProxyServer.getInstance().getPluginManager().callEvent(
                new BungeePlayerJoinEvent(uuid, message.getUsername(), new DefaultCallback<>())
        );
    }

    private void onPlayerLeaveMessage(RedisMessages.PlayerLeaveNetwork message) {
        if (checkEventNotFromHere(message.getBase()))
            return;
        System.out.printf("Event: PlayerRemove %s.\n", message.getUsername());

        UUID uuid = parseUUID(message.getUuid().getData());
        if (uuid == null)
            return;

        ProxyServer.getInstance().getPluginManager().callEvent(
                new BungeePlayerLeaveEvent(uuid, message.getUsername(), new DefaultCallback<>())
        );
    }

    private void onPlayerServerChangeMessage(RedisMessages.PlayerChangeServer message) {
        if (checkEventNotFromHere(message.getBase()))
            return;
        System.out.printf("Event: PlayerChange %s.\n", message.getUsername());

        UUID uuid = parseUUID(message.getUuid().getData());
        if (uuid == null)
            return;

        ProxyServer.getInstance().getPluginManager().callEvent(
                new BungeePlayerServerChangeEvent(uuid, message.getUsername(), message.getOldServer(), message.getNewServer(), new DefaultCallback<>())
        );
    }

    private void onSendPlayerMessage(RedisMessages.RequestPlayerSend message) {
        if (checkEventNotFromHere(message.getBase()))
            return;
        System.out.printf("Event: RequestPlayerSend %s -> %s.\n", message.getUsername(), message.getTargetServer());

        UUID uuid = parseUUID(message.getUuid().getData());
        if (uuid == null)
            return;

        ProxyServer.getInstance().getPluginManager().callEvent(
                new BungeeRequestPlayerServerSendEvent(uuid, message.getUsername(), message.getTargetServer(), new DefaultCallback<>())
        );
    }

    private void onKickPlayerMessage(RedisMessages.RequestPlayerKick message) {
        if (checkEventNotFromHere(message.getBase()))
            return;

        UUID uuid = parseUUID(message.getUuid().getData());
        if (uuid != null)
            BungeePlugin.getBungeeApi().kickPlayer(uuid, message.getReason());

        BungeePlugin.getBungeeApi().kickPlayer(message.getUsername(), message.getReason());
    }

}


