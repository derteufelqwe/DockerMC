package de.derteufelqwe.bungeeplugin.redis;

import com.sun.istack.NotNull;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import javax.annotation.CheckForNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * The local cache implementation, which is responsible of mirroring the player data in redis.
 */
public class RedisDataCache {

    private Map<String, PlayerData> players = new HashMap<>();
    private Map<String, Integer> serverPlayerCounts = new HashMap<>();
    private Map<String, Integer> proxyPlayerCounts = new HashMap<>();


    public RedisDataCache() {

    }


    /**
     * Returns a player from the cache.
     * @param username Name of the player to get
     * @return The players data or null if he is not in the cache
     */
    @CheckForNull
    public PlayerData getPlayer(String username) {
        return this.players.get(username);
    }

    /**
     * Adds a player to the cache
     * @param playerData Player data to add
     */
    public void addPlayer(PlayerData playerData) {
        PlayerData oldData = this.players.get(playerData.getUsername());
        if (oldData != null) {
            this.decrementServerPlayerCount(oldData.getServer());
            this.decrementProxyPlayerCount(oldData.getBungeeCordId());
        }

        this.players.put(playerData.getUsername(), playerData);
        this.incrementServerPlayerCount(playerData.getServer());
        this.incrementProxyPlayerCount(playerData.getBungeeCordId());
    }

    /**
     * Removes a player from the cache
     * @param username Name of the player to remove
     * @return true if the player was removed, otherwise false
     */
    public boolean removePlayer(String username) {
        PlayerData playerData = this.players.remove(username);
        if (playerData != null) {
            this.decrementServerPlayerCount(playerData.getServer());
            this.decrementProxyPlayerCount(playerData.getBungeeCordId());
        }

        return playerData != null;
    }

    /**
     * Checks if a player is in the cache
     * @param username Player to check if in the cache.
     * @return Yes or no
     */
    public boolean containsPlayer(String username) {
        return this.players.containsKey(username);
    }

    /**
     * Returns a list of player on a certain server.
     * @param servername Name of the server to filter
     * @return The players on the server
     */
    public List<PlayerData> getPlayersOnServer(String servername) {
        return this.players.entrySet().stream()
                .filter(e -> e.getValue().getServer().equals(servername))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Gets all players connected through a certain BungeeCord instance
     * @param bungeeId The id of the BungeeCord to get the player for
     */
    public List<PlayerData> getPlayersOnProxy(String bungeeId) {
        return this.players.entrySet().stream()
                .filter(e -> e.getValue().getBungeeCordId().equals(bungeeId))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Increments the player count on a certain server
     * @param serverName Name of the server to increment
     */
    public void incrementServerPlayerCount(String serverName) {
        this.serverPlayerCounts.put(serverName, this.serverPlayerCounts.getOrDefault(serverName, 0) + 1);
    }

    /**
     * Decrements the player count on a certain server
     * @param serverName Name of the server to decrement
     */
    public void decrementServerPlayerCount(String serverName) {
        this.serverPlayerCounts.put(serverName, this.serverPlayerCounts.getOrDefault(serverName, 1) - 1);
    }

    /**
     * Increments the player count on a proxy
     * @param proxyname
     */
    public void incrementProxyPlayerCount(String proxyname) {
        this.proxyPlayerCounts.put(proxyname, this.proxyPlayerCounts.getOrDefault(proxyname, 0) + 1);
    }

    /**
     * Decrements the player count on a proxy
     * @param proxyName
     */
    public void decrementProxyPlayerCount(String proxyName) {
        this.proxyPlayerCounts.put(proxyName, this.proxyPlayerCounts.getOrDefault(proxyName, 1) - 1);
    }

    /**
     * Updates the server a player is on
     * @param username
     * @param newServer
     */
    public void updatePlayerServer(String username, String newServer) {
        String oldServer = this.players.get(username).getServer();
        this.players.get(username).setServer(newServer);

        this.decrementServerPlayerCount(oldServer);
        this.incrementServerPlayerCount(newServer);
    }

    /**
     * Returns the player count on a certain server
     * @param serverName
     * @return
     */
    public int getServerPlayerCount(String serverName) {
        return this.serverPlayerCounts.getOrDefault(serverName, 0);
    }

    /**
     * Returns the player count on a certain bungee proxy
     * @param proxyName
     * @return
     */
    public int getProxyPlayerCount(String proxyName) {
        return this.proxyPlayerCounts.getOrDefault(proxyName, 0);
    }


    @Getter
    public static class PlayerData {

        private String username;
        private String uuid;
        private String address;
        @Setter private String server;
        @Setter private String bungeeCordId = BungeePlugin.BUNGEECORD_ID;

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

            Server server = player.getServer();
            if (server == null)
                return;
            ServerInfo serverInfo = server.getInfo();
            if (serverInfo == null)
                return;
            this.server = serverInfo.getName();
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

        /**
         * Returns all class fields, which are saved in redis.
         */
        public static String[] getFields() {
            return new String[]{"username", "uuid", "address", "server", "bungeeCordId"};
        }

    }

}
