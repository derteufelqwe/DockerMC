package de.derteufelqwe.bungeeplugin.eventhandlers;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.events.*;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.bungeeplugin.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class BungeeEventsHandler implements Listener {

    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoinNetwork(BungeePlayerJoinEvent event) {
        System.out.println("Player " + event.getPlayerName() + " joined.");
        this.redisDataManager.loadPlayer(event.getPlayerName());
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLeaveNetwork(BungeePlayerLeaveEvent event) {
        System.out.println("Player " + event.getPlayerName() + " left.");
        this.redisDataManager.removePlayer(event.getPlayerName());
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerServerChange(BungeePlayerServerChangeEvent event) {
        System.out.printf("Player %s changed server %s -> %s.\n", event.getPlayerName(), event.getOldServer(), event.getNewServer());
        this.redisDataManager.updatePlayersServer(event.getPlayerName(), event.getNewServer());
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRequestPlayerSend(BungeeRequestPlayerServerSendEvent event) {
        ServerInfo serverInfo = Utils.getServers().get(event.getTargetServer());
        if (serverInfo == null) {
            System.err.printf("Trying to connect to invalid server %s.\n", event.getTargetServer());
            return;
        }

        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(event.getPlayerName());
        if (player == null) {
            System.err.printf("Couldn't find user %s to connect to %s.\n", event.getPlayerName(), event.getTargetServer());
            return;
        }

        player.connect(serverInfo, ServerConnectEvent.Reason.PLUGIN);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRequestPlayerKick(BungeeRequestPlayerKickEvent event) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(event.getPlayerName());

        if (player == null) {
            return;
        }

        TextComponent kickMessage = new TextComponent(ChatColor.RED + "You got kicked!");
        if (event.getReason() != null) {
            kickMessage.addExtra(" Reason: '" + event.getReason() + "'.");
        }

        player.disconnect(kickMessage);
    }

}
