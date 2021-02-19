package de.derteufelqwe.bungeeplugin.redis;

import com.google.protobuf.InvalidProtocolBufferException;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.events.*;
import de.derteufelqwe.bungeeplugin.runnables.DefaultCallback;
import de.derteufelqwe.bungeeplugin.runnables.SessionRunnable;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.protobuf.RedisMessages;
import net.md_5.bungee.api.ProxyServer;
import org.hibernate.Session;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.CheckForNull;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Receives the published messages on redis and dispatches them into BungeeCord events
 * This class should run in a different thread since the {@link #run()} method is blocking
 */
public class RedisPublishListener extends BinaryJedisPubSub implements Runnable {

    private JedisPool jedisPool = BungeePlugin.getRedisPool().getJedisPool();


    public RedisPublishListener() {

    }


    @Override
    public void run() {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.subscribe(this, Constants.REDIS_MESSAGES_CHANNEL);
        }
    }


    @Override
    public void onMessage(byte[] channel, byte[] message) {
        try {
            RedisMessages.RedisMessage msg = RedisMessages.RedisMessage.parseFrom(message);


            if (msg.hasPlayerJoinNetwork()) {
                this.onPlayerJoinMessage(msg.getPlayerJoinNetwork());
            }

            if (msg.hasPlayerLeaveNetwork()) {
                this.onPlayerLeaveMessage(msg.getPlayerLeaveNetwork());
            }

            if (msg.hasPlayerChangeServer()) {
                this.onPlayerServerChangeMessage(msg.getPlayerChangeServer());
            }

            if (msg.hasRequestPlayerKick()) {
                this.onKickPlayerMessage(msg.getRequestPlayerKick());
            }

            if (msg.hasRequestPlayerSend()) {
                this.onSendPlayerMessage(msg.getRequestPlayerSend());
            }

            if (msg.hasMcServerStarted()) {
                this.onMCServerStarted(msg.getMcServerStarted());
            }

            if (msg.hasMcServerStopped()) {
                this.onMCServerStopped(msg.getMcServerStopped());
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
            System.err.printf("Got invalid uuid '%s'.\n", uuid);
            return null;
        }
    }

    // -----  BungeeCord Message handlers  -----

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

    // -----  Infrastructure Message handlers  -----

    private void onMCServerStarted(RedisMessages.MCServerStarted message) {
        String containerId = message.getContainerId();

        ProxyServer.getInstance().getScheduler().runAsync(BungeePlugin.PLUGIN, new SessionRunnable() {
            @Override
            public void run(Session session) {
                DBContainer container = session.get(DBContainer.class, containerId);

                try {
                    DMCServerAddEvent addServerEvent = new DMCServerAddEvent(
                            container.getMinecraftServerName(),
                            (Inet4Address) Inet4Address.getByName(container.getIp()),
                            container.getId(),
                            container.getService().getId(),
                            new DefaultCallback<>()
                    );
                    addServerEvent.callEvent();

                } catch (UnknownHostException e) {
                    e.printStackTrace(System.err);
                }
            }
        });
    }

    private void onMCServerStopped(RedisMessages.MCServerStopped message) {
        String containerId = message.getContainerId();

        ProxyServer.getInstance().getScheduler().runAsync(BungeePlugin.PLUGIN, new SessionRunnable() {
            @Override
            public void run(Session session) {
                DBContainer container = session.get(DBContainer.class, containerId);

                try {
                    DMCServerRemoveEvent removeServerEvent = new DMCServerRemoveEvent(
                            container.getMinecraftServerName(),
                            (Inet4Address) Inet4Address.getByName(container.getIp()),
                            container.getId(),
                            container.getService().getId(),
                            new DefaultCallback<>()
                    );
                    removeServerEvent.callEvent();

                } catch (UnknownHostException e) {
                    e.printStackTrace(System.err);
                }
            }
        });
    }

}
