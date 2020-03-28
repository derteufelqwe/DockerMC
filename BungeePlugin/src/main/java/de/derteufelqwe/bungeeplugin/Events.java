package de.derteufelqwe.bungeeplugin;

import com.google.common.base.Utf8;
import com.google.common.collect.Iterables;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;

/**
 * Reihenfolge: Join -> Connect
 */

public class Events implements Listener {

    /**
     * Executed when a player connects to a server. This will send the player to the first available server.
     */
    @EventHandler
    public void playerConnect(ServerConnectEvent event) {

        if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY) {
            Collection<ServerInfo> servers = Utils.getServers().values();

            if (servers.size() > 0) {
                ServerInfo serverInfo = Iterables.get(servers, 0);
                event.setTarget(serverInfo);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerDisconnectEvent event) {
        event.getPlayer().setReconnectServer(null);
    }

}
