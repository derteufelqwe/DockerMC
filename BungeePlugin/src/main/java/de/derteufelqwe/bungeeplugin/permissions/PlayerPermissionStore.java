package de.derteufelqwe.bungeeplugin.permissions;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.permissions.PermissionBase;
import de.derteufelqwe.commons.hibernate.objects.permissions.PlayerToPermissionGroup;
import de.derteufelqwe.commons.hibernate.objects.permissions.ServicePermission;
import org.hibernate.Session;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stores permission data for a player
 */
public class PlayerPermissionStore {

    private final SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();

    private PermissionGroupStore permissionGroupStore = new PermissionGroupStore();


    // Data: <PlayerId, <Permission> >
    private Map<UUID, Set<String>> normalPermissions = new HashMap<>();

    // Data: <PlayerId, <ServiceId, <Permissions> > >
    private Map<UUID, Map<String, Set<String>>> servicePermissions = new HashMap<>();

    // Data: <PlayerId, <GroupId> >
    private Map<UUID, Set<Long>> groups = new HashMap<>();


    public PlayerPermissionStore() {

    }


    /**
     * Initializes the permission group store
     */
    public void init() {
        this.permissionGroupStore.init();
    }


    /**
     * Loads players permission information into the cache
     * @param playerId
     */
    public void loadPlayer(UUID playerId) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer player = session.get(DBPlayer.class, playerId);

            // Group
            this.groups.put(playerId, new HashSet<>());
            if (player.getMainPermGroup() != null)
                this.groups.get(playerId).add(player.getMainPermGroup().getId());
            if (player.getAdditionPermGroups() != null)
                this.groups.get(playerId).addAll(player.getAdditionPermGroups().stream()
                        .map(PlayerToPermissionGroup::getId)
                        .collect(Collectors.toSet())
                );

            // Normal permissions
            if (player.getPermissions() != null)
                this.normalPermissions.put(playerId, player.getPermissions().stream()
                        .map(PermissionBase::getPermissionText)
                        .collect(Collectors.toSet())
                );

            // Service permissions
            this.servicePermissions.put(playerId, new HashMap<>());
            if (player.getServicePermissions() != null) {
                for (ServicePermission p : player.getServicePermissions()) {
                    String serviceId = p.getService().getId();
                    servicePermissions.get(playerId).computeIfAbsent(serviceId, k -> new HashSet<>()); // Add set if not existing

                    servicePermissions.get(playerId).get(serviceId).add(p.getPermissionText());
                }
            }

        }
    }

    /**
     * Removes players permissions from the cache
     */
    public void removePlayer(UUID playerId) {
        this.groups.remove(playerId);
        this.normalPermissions.remove(playerId);
        this.servicePermissions.remove(playerId);
    }

    /**
     * Checks if a player has a permission. This includes permission group checks
     */
    public boolean hasPermission(UUID playerId, String permission) {
        if (this.normalPermissions.get(playerId).contains(permission))
            return true;

        for (Long groupId : this.groups.get(playerId)) {
            if (this.permissionGroupStore.hasPermission(groupId, permission))
                return true;
        }

        return false;
    }

    /**
     * Additionally checks if a service bound permission exists
     */
    public boolean hasPermission(UUID playerId, String permission, String serviceId) {
        if (this.hasPermission(playerId, permission))
            return true;

        Set<String> servicePerms = this.servicePermissions.get(playerId).get(serviceId);
        if (servicePerms != null && servicePerms.contains(permission))
            return true;

        for (Long groupId : this.groups.get(playerId)) {
            if (this.permissionGroupStore.hasPermission(groupId, permission, serviceId))
                return true;
        }

        return false;
    }


}
