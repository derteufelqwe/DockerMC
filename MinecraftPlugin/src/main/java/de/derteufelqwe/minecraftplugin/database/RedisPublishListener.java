package de.derteufelqwe.minecraftplugin.database;

import com.google.protobuf.InvalidProtocolBufferException;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.protobuf.RedisMessages;
import de.derteufelqwe.minecraftplugin.events.ServerAddEvent;
import de.derteufelqwe.minecraftplugin.events.ServerRemoveEvent;
import de.derteufelqwe.minecraftplugin.MinecraftPlugin;
import org.bukkit.Bukkit;
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

    private JedisPool jedisPool = MinecraftPlugin.getRedisPool().getJedisPool();
    private String localContainerId = MinecraftPlugin.getMetaData().readContainerID();


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


    @CheckForNull
    private UUID parseUUID(String uuid) {
        try {
            return UUID.fromString(uuid);

        } catch (IllegalArgumentException e) {
            System.err.printf("Got invalid uuid '%s'.\n", uuid);
            return null;
        }
    }

    // -----  Infrastructure Message handlers  -----

    private void onMCServerStarted(RedisMessages.MCServerStarted message) {
        String containerId = message.getContainerId();
        if (containerId.equals(this.localContainerId))
            return;

        Bukkit.getScheduler().runTaskAsynchronously(MinecraftPlugin.INSTANCE, new SessionRunnable(3) {
            @Override
            public void run(Session session) {
                DBContainer container = session.get(DBContainer.class, containerId);

                try {
                    ServerAddEvent addServerEvent = new ServerAddEvent(
                            container.getMinecraftServerName(),
                            container.getId(),
                            (Inet4Address) Inet4Address.getByName(container.getIp()),
                            container.getService().getId()
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
        if (containerId.equals(this.localContainerId))
            return;

        Bukkit.getScheduler().runTaskAsynchronously(MinecraftPlugin.INSTANCE, new SessionRunnable(3) {
            @Override
            public void run(Session session) {
                DBContainer container = session.get(DBContainer.class, containerId);

                try {
                    ServerRemoveEvent addServerEvent = new ServerRemoveEvent(
                            container.getMinecraftServerName(),
                            container.getId(),
                            (Inet4Address) Inet4Address.getByName(container.getIp()),
                            container.getService().getId()
                    );
                    addServerEvent.callEvent();

                } catch (UnknownHostException e) {
                    e.printStackTrace(System.err);
                }
            }
        });
    }

}
