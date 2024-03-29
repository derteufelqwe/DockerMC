package de.derteufelqwe.bungeeplugin.redis;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.exceptions.RedisCacheException;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.exceptions.InvalidStateError;
import de.derteufelqwe.commons.protobuf.RedisMessages;
import org.checkerframework.checker.units.qual.C;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Class responsible for interacting with redis
 */
public class RedisDataManager {

    private final JedisPool jedisPool = BungeePlugin.getRedisPool().getJedisPool();

    /*
     * Maps player names to their player information
     */
    private Map<String, PlayerData> playersMap = new ConcurrentHashMap<>();

    /*
     * A redis cache, that is currently only used to cache the player count for 100 ms.
     */
    private LoadingCache<Integer, String> cache = createCache();


    public RedisDataManager() {

    }


    /**
     * Initializes the cache and loads all players from redis
     */
    public void init() {
        try (Jedis jedis = this.jedisPool.getResource()) {
            Set<String> playerKeys = jedis.keys("players#*");

            // Request all available players from redis in an atomic transaction
            Transaction tx = jedis.multi();
            for (String player : playerKeys) {
                tx.hgetAll(player);
            }
            List<Object> responses = tx.exec();

            // Load the players from the response into the cache
            for (Object r : responses) {
                if (r == null) {
                    System.err.println("Received null value from redis");
                    continue;
                }

                Map<String, String> data = (Map<String, String>) r;
                String userName = data.get("username");

                if (userName != null) {
                    this.playersMap.put(userName, new PlayerData(data));

                } else {
                    System.err.println("Read incomplete player data from redis for player");
                }
            }
        }

    }

    /**
     * Creates the cache that is responsible for retrieving the player count from redis
     */
    private LoadingCache<Integer, String> createCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofMillis(100L))
                .build(new CacheLoader<Integer, String>() {
                    @Override
                    public String load(Integer key) throws Exception {
                        switch (key) {
                            case 1:
                                try (Jedis jedis = jedisPool.getResource()) {
                                    String value = jedis.get("playerCount");
                                    if (value == null)
                                        return "0";
                                    return value;
                                }

                            default:
                                throw new RuntimeException("Cache got invalid key " + key);
                        }
                    }
                });
    }

    // -----  Player methods  -----

    /**
     * Gets a player from the cache only. If the player is online and not in the cache sth. went wrong.
     *
     * @return The player data or null
     */
    @CheckForNull
    public PlayerData getPlayer(@Nullable String username) {
        if (username == null) {
            return null;
        }

        return this.playersMap.get(username);
    }

    @CheckForNull
    public PlayerData getPlayer(UUID uuid) throws InvalidStateError {
        List<PlayerData> players = this.playersMap.entrySet().stream()
                .filter(e -> e.getValue().getUuid().equals(uuid.toString()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        if (players.size() == 0) {
            return null;

        } else if (players.size() == 1) {
            return players.get(0);

        } else {
            throw new InvalidStateError("Found multiple players for uuid %s.", uuid.toString());
        }
    }

    /**
     * Loads a player from redis into the cache.
     *
     * @param username Name of the player to load.
     * @return Found or not
     */
    public boolean loadPlayer(String username) throws RedisCacheException {
        try (Jedis jedis = this.jedisPool.getResource()) {
            Map<String, String> data = jedis.hgetAll("players#" + username);

            if (data != null && data.size() > 0) {
                this.playersMap.put(data.get("username"), new PlayerData(data));
                return true;
            }

            throw new RedisCacheException("Can't load player %s. Not found in redis.", username);
        }
    }

    /**
     * Removes a player from the network.
     * Removes it from the local cache and redis and notifies other BungeeCord instances about it.
     *
     * @param username Name of the player to remove
     */
    public void removePlayer(String username) {
        this.playersMap.remove(username);
    }

    /**
     * Updates the server a player is on.
     * Only called on the proxy the player is changing the server
     *
     * @param username  Name of the player
     * @param newServer New servername of the player
     */
    public void updatePlayersServer(String username, String newServer) throws RedisCacheException {
        PlayerData data = this.playersMap.get(username);
        if (data == null) {
            throw new RedisCacheException("Player %s not found in the cache.", username);
        }

        data.setServer(newServer);
    }

    // -----  Server methods  -----

    /**
     * Returns the number of players on the whole network. The result is cached, so many concurrent pings don't stress
     * redis too much
     *
     * @return The overall player count
     * @throws RedisCacheException When the redis value is an invalid integer
     */
    public int getOverallPlayerCount() throws RedisCacheException {
        try {
            String value = cache.get(1);
            return Integer.parseInt(value);

        } catch (ExecutionException e) {
            e.printStackTrace(System.err);
            return 0;

        } catch (CacheLoader.InvalidCacheLoadException | NullPointerException e) {
            return 0;

        } catch (NumberFormatException e2) {
            throw new RedisCacheException("Redis playerCount contains non integer value.");
        }
    }

    /**
     * Returns the player count on a single server
     *
     * @param serverName Name of the server to get the count of
     * @return The player count
     */
    public int getServersPlayerCount(String serverName) throws RedisCacheException {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get("minecraft#playerCount#" + serverName);
            try {
                if (value != null)
                    return Integer.parseInt(value);
                else
                    return 0;

            } catch (NumberFormatException e2) {
                throw new RedisCacheException("Redis minecraft servers %s player count contains non integer value '%s'.", serverName, value);
            }
        }
    }

    /**
     * Returns the player count over a certain BungeeCord instance
     *
     * @param bungeeName The name of the BungeeCord proxy like BungeeCord.1.1ks3h
     */
    public int getBungeesPlayerCount(String bungeeName) throws RedisCacheException {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get("bungee#playerCount#" + bungeeName);
            try {
                if (value != null)
                    return Integer.parseInt(value);
                else
                    return 0;

            } catch (NumberFormatException e2) {
                throw new RedisCacheException("Redis bungee servers %s player count contains non integer value '%s'.", bungeeName, value);
            }
        }
    }

    /**
     * Returns a list of players, which is connected on the named server
     *
     * @param serverName
     * @return
     */
    public List<PlayerData> getPlayerOnServer(String serverName) {
        return this.playersMap.entrySet().stream()
                .filter(e -> e.getValue().getServer().equals(serverName))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of players, which is connected through the named BungeeCord id
     *
     * @param bungeeId
     * @return
     */
    public List<PlayerData> getPlayersOnBungee(String bungeeId) {
        return this.playersMap.entrySet().stream()
                .filter(e -> e.getValue().getBungeeCordId().equals(bungeeId))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    // -----  Message methods  -----

    /**
     * Publishes a BungeePlugin known message to redis for the other nodes to receive it
     *
     * @param message
     */
    public void sendMessage(RedisMessages.RedisMessage message) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.publish(Constants.REDIS_MESSAGES_CHANNEL, message.toByteArray());
        }
    }

    public void sendMessage(RedisMessages.PlayerJoinNetwork message) {
        this.sendMessage(RedisMessages.RedisMessage.newBuilder()
                .setPlayerJoinNetwork(message)
                .build());
    }

    public void sendMessage(RedisMessages.PlayerLeaveNetwork message) {
        this.sendMessage(RedisMessages.RedisMessage.newBuilder()
                .setPlayerLeaveNetwork(message)
                .build());
    }

    public void sendMessage(RedisMessages.PlayerChangeServer message) {
        this.sendMessage(RedisMessages.RedisMessage.newBuilder()
                .setPlayerChangeServer(message)
                .build());
    }

    public void sendMessage(RedisMessages.RequestPlayerKick message) {
        this.sendMessage(RedisMessages.RedisMessage.newBuilder()
                .setRequestPlayerKick(message)
                .build());
    }

    public void sendMessage(RedisMessages.RequestPlayerSend message) {
        this.sendMessage(RedisMessages.RedisMessage.newBuilder()
                .setRequestPlayerSend(message)
                .build());
    }

}
