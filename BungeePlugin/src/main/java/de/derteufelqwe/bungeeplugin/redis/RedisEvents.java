package de.derteufelqwe.bungeeplugin.redis;

import com.google.gson.Gson;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;


public class RedisEvents implements Listener {

    private JedisPool jedisPool;


    public RedisEvents() {
        this.jedisPool = BungeePlugin.getRedisHandler().getJedisPool();
    }


    @EventHandler
    public void onProxyPingEvent(ProxyPingEvent event) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            String playerCountStr = jedis.get("playerCount");
            int playerCount = playerCountStr == null ? 0 : Integer.parseInt(playerCountStr);

            event.getResponse().setPlayers(new ServerPing.Players(100, playerCount, null));
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinEvent(PostLoginEvent event) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            ProxiedPlayer player = event.getPlayer();
            Map<String, String> map = new HashMap<>();
            map.put("uuid", player.getUniqueId().toString());
            map.put("address", player.getAddress().toString());

            jedis.hset("players#" + player.getDisplayName(), map);

            jedis.incr("playerCount");
        }
    }


    @EventHandler
    public void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            ProxiedPlayer player = event.getPlayer();

            jedis.hdel("players#" + player.getDisplayName(), "uuid", "address", "server");

            jedis.decr("playerCount");
        }
    }


    @EventHandler
    public void onPlayerServerConnect(ServerConnectedEvent event) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            ProxiedPlayer player = event.getPlayer();

            jedis.hset("players#" + player.getDisplayName(), "server", event.getServer().getInfo().getName());
        }
    }

}
