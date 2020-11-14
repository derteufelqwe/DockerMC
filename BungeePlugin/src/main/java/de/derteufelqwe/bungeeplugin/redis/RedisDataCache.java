package de.derteufelqwe.bungeeplugin.redis;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.sun.istack.NotNull;
import de.derteufelqwe.bungeeplugin.exceptions.UserNotFoundException;
import de.derteufelqwe.bungeeplugin.redis.events.RedisPlayerAddEvent;
import de.derteufelqwe.bungeeplugin.redis.events.RedisPlayerRemoveEvent;
import de.derteufelqwe.bungeeplugin.redis.events.RedisPlayerServerChangeEvent;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import javax.annotation.CheckForNull;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible to store information, which is also stored in redis.
 * These caches are used for faster data access and low load on redis.
 */
public class RedisDataCache {

    private JedisPool jedisPool;
    private LoadingCache<String, PlayerData> playerCache;
    private String bungeeCordId;


    public RedisDataCache(JedisPool jedisPool, String bungeeCordId) {
        this.jedisPool = jedisPool;
        this.playerCache = this.buildPlayerCache();
        this.bungeeCordId = bungeeCordId;
    }


    public void init() {

        try (Jedis jedis = this.jedisPool.getResource()) {
            Set<String> playerKeys = jedis.keys("players#*");
            Set<Response<Map<String, String>>> responses = new HashSet<>();

            // Load the data from redis
            Pipeline p = jedis.pipelined();
            for (String player : playerKeys) {
                responses.add(p.hgetAll(player));
            }
            p.sync();

            // Load the players to the cache
            for (Response<Map<String, String>> r : responses) {
                Map<String, String> data = r.get();
                if (data != null && data.get("username") != null) {
                    this.playerCache.put(data.get("username"), new PlayerData(r.get()));
                }
            }
        }

    }

    private LoadingCache<String, PlayerData> buildPlayerCache() {
        return CacheBuilder.newBuilder()
                .refreshAfterWrite(30, TimeUnit.MINUTES)    // ToDo: Maybe configurable
                .build(new PlayerCacheLoader(this.jedisPool));
    }


    @CheckForNull
    public PlayerData getPlayer(String name) {
        try {
            return this.playerCache.get(name);

        } catch (ExecutionException e) {
            e.printStackTrace();

        } catch (UncheckedExecutionException e2) {
            // Called when the player is not in redis
            if (e2.getCause() instanceof UserNotFoundException) {
                return null;
            }

            throw e2;
        }

        return null;
    }

    public void addPlayerToRedis(PlayerData playerData) {
        playerData.setBungeeCordId(this.bungeeCordId);
        this.playerCache.put(playerData.getUsername(), playerData);

        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.hset("players#" + playerData.getUsername(), playerData.toMap());
            jedis.publish("events#playerJoin", new RedisPlayerAddEvent(playerData.getUsername()).serialize());
        }
    }

    public void loadPlayerFromRedis(String username) {
        try {
            this.playerCache.refresh(username);

        } catch (UncheckedExecutionException e2) {
            // Called when the player is not in redis
            if (e2.getCause() instanceof UserNotFoundException) {
                // Pass
            }

            throw e2;
        }
    }

    public void removePlayerFromRedis(String username) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.hdel("players#" + username, "uuid", "address", "server", "username", "bungeeCordId");
            jedis.publish("events#playerLeave", new RedisPlayerRemoveEvent(username).serialize());
        }

        this.playerCache.invalidate(username);
    }

    public void removePlayerFromCache(String username) {
        this.playerCache.invalidate(username);
    }

    public void updatePlayersServerInRedis(ProxiedPlayer player, Server server) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.hset("players#" + player.getDisplayName(), "server", server.getInfo().getName());
            jedis.publish("events#playerServerChange", new RedisPlayerServerChangeEvent(player.getDisplayName()).serialize());
        }

        try {
            PlayerData playerData = this.playerCache.get(player.getDisplayName());
            playerData.setServer(server.getInfo().getName());
            this.playerCache.put(player.getDisplayName(), playerData);

        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }


    public int getServersPlayerCount(String serverName) {
        return (int) this.playerCache.asMap().values().stream()
                .filter(o -> o.getServer().equals(serverName))
                .count();
    }

    public int getOverallPlayerCount() {
        try (Jedis jedis = this.jedisPool.getResource()) {
            try {
                return Integer.parseInt(jedis.get("playerCount"));

            } catch (NullPointerException e1) {
                return -1;
            } catch (NumberFormatException e2) {
                return -2;
            }
        }
    }

    @Getter
    public static class PlayerData {

        private String username;
        private String uuid;
        private String address;
        @Setter private String server;
        @Setter private String bungeeCordId;

        public PlayerData(@NotNull Map<String, String> input) {
            this.username = input.get("username");
            this.uuid = input.get("uuid");
            this.address = input.get("address");
            this.server = input.get("server");
            this.bungeeCordId = input.get("bungeeCordId");
        }

        public PlayerData(@NotNull ProxiedPlayer player) {
            this.username = player.getDisplayName();
            this.uuid = player.getUniqueId().toString();
            this.address = player.getAddress().toString();
        }


        public Map<String, String> toMap() {
            Map<String, String> map = new HashMap<>();

            map.put("username", this.username);
            map.put("uuid", this.uuid);
            map.put("address", this.address);
            if (this.server != null)
                map.put("server", this.server);
            if (this.bungeeCordId != null)
                map.put("bungeeCordId", this.bungeeCordId);

            return map;
        }

    }

    /**
     * Class responsible for loading new players from redis if the cache doesn't contain them
     */
    private static class PlayerCacheLoader extends CacheLoader<String, PlayerData> {

        private JedisPool jedisPool;

        public PlayerCacheLoader(JedisPool jedisPool) {
            this.jedisPool =jedisPool;
        }

        @Override
        public PlayerData load(String key) throws Exception {
            try (Jedis jedis = this.jedisPool.getResource()) {
                Map<String, String> data = jedis.hgetAll("players#" + key);

                if (data != null && data.size() > 0) {
                    return new PlayerData(data);
                }

                throw new UserNotFoundException();
            }
        }
    }

}
