package de.derteufelqwe.bungeeplugin.eventhandlers;

import de.derteufelqwe.bungeeplugin.events.BungeePlayerJoinEvent;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerLeaveEvent;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerServerChangeEvent;
import de.derteufelqwe.bungeeplugin.events.BungeeRequestPlayerServerSendEvent;
import de.derteufelqwe.bungeeplugin.utils.Utils;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeEventsHandler implements Listener {

    @EventHandler
    public void onPlayerJoinNetwork(BungeePlayerJoinEvent event) {
        System.out.println("Player " + event.getPlayerName() + " joined.");
    }


    @EventHandler
    public void onPlayerLeaveNetwork(BungeePlayerLeaveEvent event) {
        System.out.println("Player " + event.getPlayerName() + " left.");
    }


    @EventHandler
    public void onPlayerServerChange(BungeePlayerServerChangeEvent event) {
        System.out.printf("Player %s changed server %s -> %s.\n", event.getPlayerName(), event.getOldServer(), event.getNewServer());
    }


    @EventHandler
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


}
