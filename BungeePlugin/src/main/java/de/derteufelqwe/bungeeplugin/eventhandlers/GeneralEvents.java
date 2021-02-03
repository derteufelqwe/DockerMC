package de.derteufelqwe.bungeeplugin.eventhandlers;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * All event handlers that don't belong to other groups can go here.
 */
public class GeneralEvents implements Listener {

    private JedisPool jedisPool = BungeePlugin.getRedisHandler().getJedisPool();


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





}
