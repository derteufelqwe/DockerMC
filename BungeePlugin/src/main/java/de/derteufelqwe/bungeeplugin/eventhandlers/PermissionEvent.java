package de.derteufelqwe.bungeeplugin.eventhandlers;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerJoinEvent;
import de.derteufelqwe.bungeeplugin.events.BungeePlayerLeaveEvent;
import de.derteufelqwe.bungeeplugin.permissions.PermissionGroupStore;
import de.derteufelqwe.bungeeplugin.permissions.PlayerPermissionStore;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.command.ConsoleCommandSender;
import net.md_5.bungee.event.EventHandler;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Only listens for the PermissionCheckEvent event
 */
public class PermissionEvent implements Listener {

    private final PlayerPermissionStore permissionStore = new PlayerPermissionStore();


    public PermissionEvent() {
        this.permissionStore.init();
    }


    @EventHandler
    public void onCheckPermission(PermissionCheckEvent event) {
        try {
            if (event.getSender() instanceof ConsoleCommandSender) {
                event.setHasPermission(true);
                return;
            }
            ProxiedPlayer player = (ProxiedPlayer) event.getSender();

            String serviceName = BungeePlugin.getRedisDataManager().getPlayer(player.getUniqueId()).getServiceName();
            boolean hasPerm = this.permissionStore.hasPermission(player.getUniqueId(), event.getPermission(), serviceName);
            event.setHasPermission(hasPerm);

        } catch (Exception e) {
            event.setHasPermission(false);
            throw e;
        }

    }


    @EventHandler
    public void onPlayerJoinNetwork(BungeePlayerJoinEvent event) {
        ProxyServer.getInstance().getScheduler().runAsync(BungeePlugin.PLUGIN, () -> {
            this.permissionStore.loadPlayer(event.getPlayerId());
            System.out.println("Loaded perms for " + event.getPlayerName());
        });
    }

    @EventHandler
    public void onPlayerJoinNetwork(BungeePlayerLeaveEvent event) {
        this.permissionStore.removePlayer(event.getPlayerId());
        System.out.println("Removed perms for " + event.getPlayerName());
    }

}
