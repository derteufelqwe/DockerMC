package de.derteufelqwe.bungeeplugin;

import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.Value;
import de.derteufelqwe.commons.consul.CacheListener;
import de.derteufelqwe.commons.consul.ICacheChangeListener;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
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

public class Events implements Listener, ICacheChangeListener<String, Value> {

    private final Pattern RE_PLAYERLIMIT = Pattern.compile("^mcservers\\/(.+)\\/softPlayerLimit$");

    private String lobbyServerName = "";
    // ToDo: SoftPlayerLimit == -1 -> unlimited
    private Map<String, Integer> softPlayerLimits = new HashMap<>();

    private KeyValueClient kvClient;
    private KVCache kvCache;


    public Events(KeyValueClient kvClient) {
        this.kvClient = kvClient;

        this.kvCache = KVCache.newCache(kvClient, "");
        CacheListener<String, Value> cacheListener = new CacheListener<>();
        cacheListener.addListener(this);
        kvCache.addListener(cacheListener);
        kvCache.start();
    }

    public void stop() {
        this.kvCache.stop();
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

        if (m.matches() && m.find()) {
            String serverName = m.group(1);
            int limit = Integer.parseInt(value.getValueAsString().get());
            this.softPlayerLimits.put(serverName, limit);
            System.out.println("Setting softPlayerLimit of server " + serverName + " to " + limit);

        } else if (key.equals("system/lobbyServerName")) {
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
     * Executed when a player connects to a server. This will send the player to the first available server.
     */
    @EventHandler
    public void playerConnect(ServerConnectEvent event) {
        if (event.getReason() != ServerConnectEvent.Reason.JOIN_PROXY) {
            return;
        }

        if (this.lobbyServerName == null || this.lobbyServerName == "") {
            System.err.println("No lobby server found.");
            event.getPlayer().disconnect(new TextComponent(ChatColor.RED + "Couldn't identify name of lobby server."));
            return;
        }

        List<ServerInfo> servers = Utils.getServers().values().stream()
                .filter(s -> s.getName().startsWith(this.lobbyServerName))
                .sorted(Comparator.comparing(ServerInfo::getName))  // Sort by Name
                .collect(Collectors.toList());

        // Connect user to server
        if (servers.size() > 0) {
            for (ServerInfo serverInfo : servers) {
                int playerLimit = this.softPlayerLimits.getOrDefault(this.lobbyServerName, -1);

                if (playerLimit == -1) {
                    event.setTarget(serverInfo);
                    return;

                } else if (serverInfo.getPlayers().size() < playerLimit) {
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

    @EventHandler
    public void onPing(ProxyPingEvent event) {

    }

}
