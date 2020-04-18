package de.derteufelqwe.bungeeplugin;

import com.google.common.base.Utf8;
import com.google.common.collect.Iterables;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reihenfolge: Join -> Connect
 */

public class Events implements Listener {

    private String lobbyServerName;
    private Integer lobbySoftPlayerLimit;

    public Events() {
        // ToDo: Comment in or refactor
//        Map<String, Object> config = Utils.requestConfigFile(Constants.Configs.INFRASTRUCTURE);
//        Map<String, String> lobbyconfig = (Map<String, String>) config.get("lobbyPool");
//        this.lobbyServerName = lobbyconfig.get("name");
//        this.lobbySoftPlayerLimit = (Integer) (Object) lobbyconfig.get("softPlayerLimit");
        this.lobbyServerName = "Lobby";
        this.lobbySoftPlayerLimit = 2;
    }

    /**
     * Executed when a player connects to a server. This will send the player to the first available server.
     */
    @EventHandler
    public void playerConnect(ServerConnectEvent event) {
        System.out.println("Connect");
        if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY) {
            List<ServerInfo> servers = Utils.getServers().values().stream()
                    .filter(s -> s.getName().startsWith(this.lobbyServerName))
                    .collect(Collectors.toList());

            Collections.sort(servers, new Comparator<ServerInfo>() {
                @Override
                public int compare(ServerInfo o1, ServerInfo o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

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
    }

    @EventHandler
    public void onQuit(PlayerDisconnectEvent event) {
        event.getPlayer().setReconnectServer(null);
    }

}
