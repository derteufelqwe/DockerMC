package de.derteufelqwe.bungeeplugin;

import com.google.common.base.Utf8;
import com.google.common.collect.Iterables;
import com.ibm.etcd.client.kv.KvClient;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import de.derteufelqwe.commons.Constants;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.event.EventHandler;

import java.net.Inet4Address;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reihenfolge: Join -> Connect
 */

public class Events implements Listener {

    private KeyValueClient kvClient;

    private String lobbyServerName = "";
    private Integer lobbySoftPlayerLimit = 0;


    public Events(Consul consul) {
        kvClient = consul.keyValueClient();

        Optional<String> serverNameOpt = kvClient.getValueAsString("system/lobbyServerName");
        if (!serverNameOpt.equals(Optional.empty())) {
            this.lobbyServerName = serverNameOpt.get();

        } else {
            System.err.println("[Fatal Error] No Lobbyname");
            ProxyServer.getInstance().stop("[Fatal Error] No Lobbyname");
        }

        Optional<String> playerLimitOpt = kvClient.getValueAsString("mcservers/" + lobbyServerName + "/softPlayerLimit");
        if (!playerLimitOpt.equals(Optional.empty())) {
            this.lobbySoftPlayerLimit = Integer.parseInt(playerLimitOpt.get());

        } else {
            System.err.println("[Fatal Error] No softPlayerLimit");
            ProxyServer.getInstance().stop("[Fatal Error] No softPlayerLimit for lobbyserver " + lobbyServerName + ".");
        }
    }

    /**
     * Executed when a player connects to a server. This will send the player to the first available server.
     */
    @EventHandler
    public void playerConnect(ServerConnectEvent event) {
        if (event.getReason() != ServerConnectEvent.Reason.JOIN_PROXY) {
            return;
        }

        List<ServerInfo> servers = Utils.getServers().values().stream()
                .filter(s -> s.getName().startsWith(this.lobbyServerName))
                .sorted(Comparator.comparing(ServerInfo::getName))  // Sort by Name
                .collect(Collectors.toList());

        if (servers.size() > 0) {
            for (ServerInfo serverInfo : servers) {
                if (serverInfo.getPlayers().size() < this.lobbySoftPlayerLimit) {
                    event.setTarget(serverInfo);
                    return;
                }
            }

            event.getPlayer().disconnect(new TextComponent(ChatColor.RED + "Server has no free slots in the lobby."));
        }

        event.getPlayer().disconnect(new TextComponent(ChatColor.RED + "No lobby servers found."));
    }

    @EventHandler
    public void onQuit(PlayerDisconnectEvent event) {
        event.getPlayer().setReconnectServer(null);
    }

}
