package de.derteufelqwe.bungeeplugin.eventhandlers;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
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

    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();


    /**
     * Returns the server information on ping.
     */
    @EventHandler
    public void onProxyPingEvent(ProxyPingEvent event) {
        int playerCount = redisDataManager.getOverallPlayerCount();
        int playerLimit = ProxyServer.getInstance().getConfig().getPlayerLimit();

        playerLimit = playerLimit < 0 ? 65535 : playerLimit;
        event.getResponse().setPlayers(new ServerPing.Players(playerLimit, playerCount, null));
    }





}
