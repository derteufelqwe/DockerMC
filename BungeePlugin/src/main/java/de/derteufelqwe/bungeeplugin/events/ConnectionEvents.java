package de.derteufelqwe.bungeeplugin.events;

import com.orbitz.consul.model.kv.Value;
import de.derteufelqwe.bungeeplugin.utils.Utils;
import de.derteufelqwe.commons.consul.ICacheChangeListener;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reihenfolge: Join -> Connect
 */

public class ConnectionEvents implements Listener, ICacheChangeListener<String, Value> {

    private final Pattern RE_PLAYERLIMIT = Pattern.compile("^mcservers\\/(.+)\\/softPlayerLimit$");

    private  String lobbyServerName = "";
    // ToDo: SoftPlayerLimit == -1 -> unlimited
    private Map<String, Integer> softPlayerLimits = new HashMap<>();


    public ConnectionEvents() {

    }


    // -----  KV Listener  -----
    @Override
    public void onAddEntry(String key, Value value) {
        System.out.println("Add " + key + " -> " + value);
        this.setValue(key, value);
    }

    @Override
    public void onModifyEntry(String key, Value value) {
        System.out.println("Modify " + key + " -> " + value);
        this.setValue(key, value);
    }

    @Override
    public void onRemoveEntry(String key, Value value) {
        System.out.println("Remove " + key + " -> " + value);
        this.unsetValue(key, value);
    }


    private void setValue(String key, Value value) {
        Matcher m = RE_PLAYERLIMIT.matcher(key);

        if (m.matches()) {
            String serverName = m.group(1);
            int limit = Integer.parseInt(value.getValueAsString().get());
            this.softPlayerLimits.put(serverName, limit);
            System.out.println("Setting softPlayerLimit of server " + serverName + " to " + limit);

        }

        if (key.equals("system/lobbyServerName")) {
            String name = value.getValueAsString().get();
            this.lobbyServerName = name;
            System.out.println("Setting lobbyServerName to " + name);
        }
    }

    private void unsetValue(String key, Value value) {
        Matcher m = RE_PLAYERLIMIT.matcher(key);

        if (m.matches() && m.find()) {
            String serverName = RE_PLAYERLIMIT.matcher(key).group(1);
            this.softPlayerLimits.remove(serverName);
            System.out.println("Unsetting ");

        } else if (key.equals("system/lobbyServerName")) {
            this.lobbyServerName = null;
        }
    }


    /**
     * Executed when a player connects to a server.
     */
    @EventHandler
    public void playerConnect(ServerConnectEvent event) {
        ServerConnectEvent.Reason reason = event.getReason();

        // Send all players to the lobby if they join the network.
        if (reason == ServerConnectEvent.Reason.JOIN_PROXY) {
            this.connectPlayerToLobby(event);
            return;
        }

        // Send the Player to the best lobby if they connect to the special "toLobby" server
        if (reason == ServerConnectEvent.Reason.COMMAND || reason == ServerConnectEvent.Reason.PLUGIN_MESSAGE) {
            if (event.getTarget().getName().equals("toLobby")) {
                this.connectPlayerToLobby(event);
                return;
            }
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

    }

    /**
     *  This will send the player to the first available server.
     */
    private void connectPlayerToLobby(ServerConnectEvent event) {
        if (this.lobbyServerName == null || this.lobbyServerName.equals("")) {
            System.err.println("[System][Critical] No lobby server found.");
            event.getPlayer().disconnect(new TextComponent(ChatColor.RED + "Can't identify name of lobby server."));
            return;
        }

        List<ServerInfo> servers = Utils.getServers().values().stream()
                .filter(s -> s.getName().startsWith(this.lobbyServerName))
                .sorted(Comparator.comparing(ServerInfo::getName))  // Sort by Name
                .collect(Collectors.toList());

        Integer playerLimit = this.softPlayerLimits.get(this.lobbyServerName);
        if (playerLimit != null && playerLimit == -1) {
            playerLimit = Integer.MAX_VALUE;
        }

        ProxiedPlayer player = event.getPlayer();

        // Connect user to server
        if (servers.size() > 0) {
            for (ServerInfo serverInfo : servers) {
                if (playerLimit == null) {
                    player.disconnect(new TextComponent(ChatColor.RED + "[Error] Lobby server has no player limit configured (" + this.lobbyServerName + ")."));
                    return;
                }

                if (playerLimit == -1) {
                    event.setTarget(serverInfo);
                    return;

                }

                if (serverInfo.getPlayers().size() < playerLimit) {
                    event.setTarget(serverInfo);
                    return;
                }
            }

            event.getPlayer().disconnect(new TextComponent(ChatColor.RED + "Server has no free slots in the lobby."));
        }

        event.getPlayer().disconnect(new TextComponent(ChatColor.RED + "[Error] No lobby servers found."));
    }


    @EventHandler
    public void onQuit(PlayerDisconnectEvent event) {
        event.getPlayer().setReconnectServer(null);
    }

}
