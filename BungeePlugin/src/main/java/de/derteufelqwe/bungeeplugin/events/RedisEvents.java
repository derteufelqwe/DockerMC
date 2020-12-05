package de.derteufelqwe.bungeeplugin.events;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.RedisDataCache;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


public class RedisEvents implements Listener {

    private JedisPool jedisPool = BungeePlugin.getRedisHandler().getJedisPool();
    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();


    public RedisEvents() {
    }


    /**
     * Returns the server information on ping.
     */
    @EventHandler
    public void onProxyPingEvent(ProxyPingEvent event) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            String playerCountStr = jedis.get("playerCount");
            int playerCount = playerCountStr == null ? 0 : Integer.parseInt(playerCountStr);
            int playerLimit = ProxyServer.getInstance().getConfig().getPlayerLimit();
            playerLimit = playerLimit < 0 ? 65535 : playerLimit;
            event.getResponse().setPlayers(new ServerPing.Players(playerLimit, playerCount, null));
        }
    }

    /**
     * Adds a player to redis when he joins the network
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinEvent(PostLoginEvent event) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            this.redisDataManager.addPlayer(new RedisDataCache.PlayerData(event.getPlayer()));

            jedis.incr("playerCount");
        }
    }

    /**
     * Removes a player from redis when he disconnects from the network
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            this.redisDataManager.removePlayer(event.getPlayer().getDisplayName());

            jedis.decr("playerCount");
        }
    }

    /**
     * Changes the server information for the player, when he changes the server
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerServerConnect(ServerConnectedEvent event) {
        this.redisDataManager.updatePlayersServer(event.getPlayer().getDisplayName(), event.getServer().getInfo().getName());
    }

}
