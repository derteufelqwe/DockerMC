package de.derteufelqwe.bungeeplugin.eventhandlers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.istack.NotNull;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.bungeeplugin.utils.Utils;
import de.derteufelqwe.commons.Constants;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Reihenfolge: Join -> Connect
 */

public class ConnectionEvents implements Listener {

    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();
    private JedisPool jedisPool = BungeePlugin.getRedisPool().getJedisPool();

    private LoadingCache<Integer, String> cache = createCache();


    public ConnectionEvents() {

    }


    private LoadingCache<Integer, String> createCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofMillis(100L))
                .build(new CacheLoader<Integer, String>() {
                    @Override
                    public String load(Integer key) throws Exception {
                        switch (key) {
                            case 1:
                                try (Jedis jedis = jedisPool.getResource()) {
                                    String value = jedis.get(Constants.REDIS_KEY_LOBBYSERVER);
                                    if (value == null)
                                        return "";
                                    return value;
                                }

                            default:
                                throw new RuntimeException("Cache got invalid key " + key);
                        }
                    }
                });
    }


    /**
     * Redirects the player to a fitting lobby server if required and possible.
     */
    @EventHandler
    public void playerConnect(ServerConnectEvent event) {
        ServerConnectEvent.Reason reason = event.getReason();

        // Send all players to the lobby if they join the network.
        if (reason == ServerConnectEvent.Reason.JOIN_PROXY) {
            this.connectPlayerToLobby(event);
            return;
        }

        // Sends the player to the best lobby if they need a fallback server
        if (reason == ServerConnectEvent.Reason.LOBBY_FALLBACK) {
            this.connectPlayerToLobby(event);
            return;
        }

        // Send the player to the lobby if his server goes down
        if (reason == ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT) {
            this.connectPlayerToLobby(event);
            return;
        }

        // Redirect the "default" server to the lobby
        if (event.getTarget().getName().equals("default")) {
            this.connectPlayerToLobby(event);
            return;
        }

        // Send the Player to the best lobby if they connect to the special "toLobby" server
        if (event.getTarget().getName().equals("toLobby")) {
            this.connectPlayerToLobby(event);
        }

    }

    /**
     * Removes information about a players last connected to server so.
     */
    @EventHandler
    public void onQuit(PlayerDisconnectEvent event) {
        event.getPlayer().setReconnectServer(null);
    }

    // -----  Utility methods  -----

    /**
     * This will send the player to the first available server.
     */
    private void connectPlayerToLobby(ServerConnectEvent event) {
        String lobbyServerName = this.getLobbyServerName();

        if (lobbyServerName.equals("")) {
            System.err.println("[System][Critical] No lobby server found.");
            event.getPlayer().disconnect(new TextComponent(ChatColor.RED + "Can't identify name of lobby server."));
            return;
        }

        List<ServerInfo> servers = Utils.getServers().values().stream()
                .filter(s -> s.getName().startsWith(lobbyServerName))
                .sorted(Comparator.comparing(ServerInfo::getName))  // Sort by Name
                .collect(Collectors.toList());

        Integer playerLimit = 10;
        if (playerLimit != null && playerLimit == -1) {
            playerLimit = Integer.MAX_VALUE;
        }

        ProxiedPlayer player = event.getPlayer();

        // Connect user to server
        if (servers.size() > 0) {
            for (ServerInfo serverInfo : servers) {
                if (playerLimit == null) {
                    player.disconnect(new TextComponent(ChatColor.RED + "[Error] Lobby server has no player limit configured (" + lobbyServerName + ")."));
                    return;
                }

                if (redisDataManager.getServersPlayerCount(serverInfo.getName()) < playerLimit) {
                    event.setTarget(serverInfo);
                    return;
                }
            }

            event.getPlayer().disconnect(new TextComponent(ChatColor.RED + "Server has no free slots in the lobby."));
        }

        event.getPlayer().disconnect(new TextComponent(ChatColor.RED + "[Error] No lobby servers found."));
    }

    /**
     * Tries to get the lobby server name from redis
     *
     * @return The name or "" if an error occurred.
     */
    @NotNull
    private String getLobbyServerName() {
        try {
            return cache.get(1);

        } catch (ExecutionException e) {
            e.printStackTrace();
            return "";
        }
    }


}
