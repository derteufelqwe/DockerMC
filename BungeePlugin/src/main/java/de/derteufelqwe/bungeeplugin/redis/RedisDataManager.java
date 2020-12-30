package de.derteufelqwe.bungeeplugin.redis;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.exceptions.RedisCacheException;
import redis.clients.jedis.*;

import javax.annotation.CheckForNull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Class responsible for interacting with redis
 */
public class RedisDataManager {

    private final JedisPool jedisPool = BungeePlugin.getRedisHandler().getJedisPool();

    private Map<String, PlayerData> playersMap = new ConcurrentHashMap<>();


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

    // -----  Player methods  -----

    /**
     * Gets a player from the cache only. If the player is online and not in the cache sth. went wrong.
     *
     * @return The player data or null
     */
    @CheckForNull
    public PlayerData getPlayer(String username) {
        return this.playersMap.get(username);
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
     * Returns the number of players on the whole network.
     *
     * @return The overall player count
     * @throws RedisCacheException When the redis value is an invalid integer
     */
    public int getOverallPlayerCount() throws RedisCacheException {
        try (Jedis jedis = this.jedisPool.getResource()) {
            String value = jedis.get("playerCount");
            try {
                return Integer.parseInt(value);

            } catch (NullPointerException e1) {
                return 0;

            } catch (NumberFormatException e2) {
                throw new RedisCacheException("Redis playerCount contains non integer value '%s'.", value);
            }
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
     * @param data
     */
    public void sendMessage(RedisPubSubData data) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.publish("messages#" + data.getMessageType(), data.serialize());
        }
    }

}
