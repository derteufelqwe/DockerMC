package de.derteufelqwe.bungeeplugin.eventhandlers;

import org.jetbrains.annotations.Nullable;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.events.*;
import de.derteufelqwe.bungeeplugin.redis.PlayerData;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.bungeeplugin.utils.ServerInfoStorage;
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

import java.net.InetSocketAddress;

public class BungeeEventsHandler implements Listener {

    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();
    private ServerInfoStorage serverInfoStorage = BungeePlugin.getServerInfoStorage();


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
        System.out.printf("Player %s changed server from %s -> %s.\n", event.getPlayerName(), event.getOldServer(), event.getNewServer());
        this.redisDataManager.updatePlayersServer(event.getPlayerName(), event.getNewServer());
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRequestPlayerSend(BungeeRequestPlayerServerSendEvent event) {
        PlayerData playerData = this.redisDataManager.getPlayer(event.getPlayerId());
        if (playerData == null)
            return;

        // Only send players, which are on this proxy
        if (!playerData.getBungeeCordId().equals(BungeePlugin.BUNGEECORD_ID))
            return;

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
        ProxiedPlayer uuidPlayer = ProxyServer.getInstance().getPlayer(event.getPlayerId());
        if (uuidPlayer != null)
            this.kickPlayer(uuidPlayer, event.getReason());

        ProxiedPlayer namePlayer = ProxyServer.getInstance().getPlayer(event.getPlayerName());
        // Only kick the named player when the uuid player is null or the players are not equal
        if (namePlayer != null && (uuidPlayer == null || !uuidPlayer.equals(namePlayer)))
            this.kickPlayer(namePlayer, event.getReason());

    }

    private void kickPlayer(ProxiedPlayer player, @Nullable String reason) {
        TextComponent kickMessage = new TextComponent(ChatColor.RED + "You got kicked!");
        if (reason != null) {
            kickMessage.addExtra(" Reason: '" + reason + "'.");
        }

        player.disconnect(kickMessage);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAddServer(DMCServerAddEvent event) {
        ProxyServer.getInstance().getConfig().addServer(ProxyServer.getInstance().constructServerInfo(
                event.getServername(), new InetSocketAddress(event.getIp(), 25565),
                "Motd", false
        ));
        serverInfoStorage.set(event.getServername(), new ServerInfoStorage.Infos(event.getContainerId(), event.getServiceId()));

        System.out.printf("Added Server '%s' (%s).\n", event.getServername(), event.getContainerId());
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRemoveServer(DMCServerRemoveEvent event) {
        ProxyServer.getInstance().getConfig().removeServerNamed(event.getServername());
        serverInfoStorage.remove(event.getServername());

        System.out.printf("Removed Server '%s' (%s).\n", event.getServername(), event.getContainerId());
    }

}
