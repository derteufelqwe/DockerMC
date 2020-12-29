package de.derteufelqwe.bungeeplugin.redis;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerJoinNetwork;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerLeaveNetwork;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerServerChange;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisRequestPlayerServerSend;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import javax.annotation.CheckForNull;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class responsible for interacting with redis
 */
public class RedisDataManager {

    private final JedisPool jedisPool = BungeePlugin.getRedisHandler().getJedisPool();
    private RedisDataCache playerCache = new RedisDataCache();


    public RedisDataManager() {

    }


    /**
     * Initializes the cache and loads all players from redis
     */
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
                    this.playerCache.addPlayer(new RedisDataCache.PlayerData(data));
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
    public RedisDataCache.PlayerData getPlayer(String username) {
        return this.playerCache.getPlayer(username);
    }

    /**
     * Adds a new player object to the network.
     * Adds it to the local cache and redis and notifies other BungeeCord instances about it.
     *
     * @param playerData Player to add
     */
    public void addPlayer(RedisDataCache.PlayerData playerData) {
        this.addPlayerLoc(playerData);

        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.hset("players#" + playerData.getUsername(), playerData.toMap());
            jedis.publish("events#playerJoin", new RedisPlayerJoinNetwork(playerData.getUsername()).serialize());
        }
    }

    /**
     * The {@link #addPlayer(RedisDataCache.PlayerData)} method for local adding only.
     */
    public void addPlayerLoc(RedisDataCache.PlayerData playerData) {
        this.playerCache.addPlayer(playerData);
    }

    /**
     * Removes a player from the network.
     * Removes it from the local cache and redis and notifies other BungeeCord instances about it.
     *
     * @param username Name of the player to remove
     */
    public void removePlayer(String username) {
        this.removePlayerLoc(username);

        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.hdel("players#" + username, RedisDataCache.PlayerData.getFields());
            jedis.publish("events#playerLeave", new RedisPlayerLeaveNetwork(username).serialize());
        }
    }

    /**
     * The {@link #removePlayer(String)} method for local removal only
     */
    public void removePlayerLoc(String username) {
        this.playerCache.removePlayer(username);
    }

    /**
     * Loads a player from redis into the cache.
     *
     * @param username Name of the player to load.
     * @return Found or not
     */
    public boolean loadPlayer(String username) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            Map<String, String> data = jedis.hgetAll("players#" + username);

            if (data != null && data.size() > 0) {
                this.playerCache.addPlayer(new RedisDataCache.PlayerData(data));
                return true;
            }

            return false;
        }
    }

    /**
     * Updates the server a player is on.
     * Only called on the proxy the player is changing the server
     *
     * @param username  Name of the player
     * @param newServer New servername of the player
     */
    public void updatePlayersServer(String username, String newServer) {
        this.playerCache.updatePlayerServer(username, newServer);

        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.hset("players#" + username, "server", newServer);
            jedis.publish("events#playerServerChange", new RedisPlayerServerChange(username).serialize());
        }
    }

    // -----  Server methods  -----

    /**
     * Returns the number of players on the whole network.
     *
     * @return If successful: the player count, otherwise:
     * -1: No player count set in redis
     * -2: Invalid entry in redis
     */
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

    /**
     * Returns the player count on a single server
     *
     * @param serverName Name of the server to get the count of
     * @return The player count
     */
    public int getServersPlayerCount(String serverName) {
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                return Integer.parseInt(jedis.get("minecraft#playerCount#" + serverName));

            } catch (NumberFormatException e1) {
                return 0;
            }
        }
    }

    /**
     * Returns the player count over a certain BungeeCord instance
     *
     * @param bungeeName The name of the BungeeCord proxy like BungeeCord.1.1ks3h
     */
    public int getBungeesPlayerCount(String bungeeName) {
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                return Integer.parseInt(jedis.get("bungee#playerCount#" + bungeeName));

            } catch (NumberFormatException e1) {
                return 0;
            }
        }
    }

    /**
     * Returns a list of players, which is connected on the named server
     *
     * @param serverName
     * @return
     */
    public List<RedisDataCache.PlayerData> getPlayerOnServer(String serverName) {
        return this.playerCache.getPlayersOnServer(serverName);
    }

    /**
     * Returns a list of players, which is connected through the named BungeeCord id
     *
     * @param bungeeId
     * @return
     */
    public List<RedisDataCache.PlayerData> getPlayersOnBungee(String bungeeId) {
        return this.playerCache.getPlayersOnProxy(bungeeId);
    }

    // -----  Message methods  -----

    /**
     * Sends a send-player message to the redis network using Redis pub sub if the player is not on the current server.
     * Sends the player specified in msg to a new server.
     *
     * @param msg Message to send
     */
    public void sendConnectMessage(RedisRequestPlayerServerSend msg) {
        if (!msg.getTargetBungee().equals(BungeePlugin.BUNGEECORD_ID)) {
            try (Jedis jedis = this.jedisPool.getResource()) {
                jedis.publish("messages#connectPlayer", msg.serialize());
            }

        } else {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(msg.getUsername());
            if (player == null) {
                System.err.printf("Player %s to send not found.\n", msg.getUsername());
                return;
            }

            ServerInfo target = ProxyServer.getInstance().getServerInfo(msg.getTargetServer());
            if (target == null) {
                System.err.printf("Send target %s not found.\n", msg.getTargetServer());
                return;
            }

            player.connect(target);
        }
    }

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
