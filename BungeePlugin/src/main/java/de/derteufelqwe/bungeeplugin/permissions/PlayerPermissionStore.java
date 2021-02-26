package de.derteufelqwe.bungeeplugin.permissions;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.permissions.Permission;
import de.derteufelqwe.commons.hibernate.objects.permissions.PlayerToPermissionGroup;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.CacheEntry;
import org.cache2k.expiry.ExpiryPolicy;
import org.hibernate.Session;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores all permissions a player has as well as their permission groups
 */
public class PlayerPermissionStore {

    private final SessionBuilder sessionBuilder;

    private final PermissionGroupStore permissionGroupStore;

    // Data: <PlayerId, <Permission> >
    private Map<UUID, Cache<PermissionCacheKey, PermissionData>> permissions = new HashMap<>();

    // Data: <PlayerId, <GroupId> >
    private Map<UUID, Map<GroupCacheKey, TimeoutList<GroupData>>> groups = new HashMap<>();


    public PlayerPermissionStore(SessionBuilder sessionBuilder) {
        this.sessionBuilder = sessionBuilder;
        this.permissionGroupStore = new PermissionGroupStore(sessionBuilder);
    }


    /**
     * Initializes the permission group store
     */
    public void init() {
        this.permissionGroupStore.init();
    }

    /**
     * Creates the permission cache for a single user, which stores their permissions
     */
    private Cache<PermissionCacheKey, PermissionData> createPermissionCache(UUID playerId) {
        return new Cache2kBuilder<PermissionCacheKey, PermissionData>() {}
                .name("perms-player-" + playerId.toString())
                .eternal(false)
                .expiryPolicy(new ExpiryPolicy<PermissionCacheKey, PermissionData>() {
                    @Override
                    public long calculateExpiryTime(PermissionCacheKey key, PermissionData value, long loadTime, CacheEntry<PermissionCacheKey, PermissionData> currentEntry) {
                        if (value.getTimeout() != null)
                            return value.getTimeout().getTime();

                        return ETERNAL;
                    }
                })
                .build();
    }


    /**
     * Loads players permission information into the cache
     *
     * @param playerId
     */
    public void loadPlayer(UUID playerId) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer player = session.get(DBPlayer.class, playerId);

            Map<GroupCacheKey, TimeoutList<GroupData>> groupDataCache = new HashMap<>();
            Cache<PermissionCacheKey, PermissionData> permissionDataCache = this.createPermissionCache(playerId);

            // Save the groups
            for (PlayerToPermissionGroup group : player.getPermissionGroups()) {
                String serviceID = group.getService() == null ? null : group.getService().getId();
                GroupCacheKey key = new GroupCacheKey(serviceID);

                groupDataCache.putIfAbsent(key, new TimeoutList<>());
                TimeoutList<GroupData> list = groupDataCache.get(key);

                GroupData data = new GroupData(
                        group.getPermissionGroup().getId(), serviceID, group.getTimeout()
                );

                long timeout = group.getTimeout() == null ? -1 : group.getTimeout().getTime();
                list.add(data, timeout);
            }

            // Save the permissions
            for (Permission perm : player.getPermissions()) {
                String serviceID = perm.getService() == null ? null : perm.getService().getId();
                PermissionCacheKey key = new PermissionCacheKey(perm.getPermissionText(), serviceID);
                PermissionData data = new PermissionData(
                        perm.getPermissionText(), serviceID, perm.getTimeout()
                );

                permissionDataCache.put(key, data);
            }

            this.groups.put(playerId, groupDataCache);
            this.permissions.put(playerId, permissionDataCache);
        }
    }

    /**
     * Removes players permissions from the cache
     */
    public void removePlayer(UUID playerId) {
        Cache<PermissionCacheKey, PermissionData> playerCache = this.permissions.get(playerId);
        if (playerCache != null)
            playerCache.clearAndClose();

        this.permissions.remove(playerId);
        this.groups.remove(playerId);
    }

    /**
     * Checks if a player has a permission. This includes permission group checks
     */
    public boolean hasPermission(UUID playerId, String permission) {
        return hasPermission(playerId, permission, null);
    }

    /**
     * Checks if a player has a service bound permission.
     *
     * @param groupServiceId Service id for searching for service bound groups
     * @param permServiceId  Service if for searching for service bound permissions in groups
     */
    private boolean hasPermission(UUID playerId, String permission, @Nullable String groupServiceId, @Nullable String permServiceId) {
        // Check normal permissions
        TimeoutList<GroupData> groups = this.groups.get(playerId).get(new GroupCacheKey(groupServiceId));
        if (groups != null) {
            for (GroupData data : groups) {
                if (this.permissionGroupStore.hasPermission(data.getGroupID(), permission, permServiceId))
                    return true;
            }
        }

        // Permission is found as is
        if (this.permissions.get(playerId).containsKey(new PermissionCacheKey(permission, permServiceId)))
            return true;

        // Check for star permission
        String[] splits = permission.split("\\.");
        if (splits.length == 1)
            return false;

        for (int i = 0; i < splits.length; i++) {
            String perm = String.join(".", Arrays.copyOfRange(splits, 0, i));
            if (perm.equals(""))
                perm += "*";
            else
                perm += ".*";

            if (this.permissions.get(playerId).containsKey(new PermissionCacheKey(perm, permServiceId)))
                return true;
        }

        return false;
    }

    /**
     * Checks if a player has a permission on a special service.
     * This includes checking if the player has
     * - a service bound group with a service bound permission
     * - a service bound group with a normal permission
     * - a normal group with a service bound permission
     * - a normal group with a normal permission
     */
    public boolean hasPermission(UUID playerId, String permission, @Nullable String servideId) {
        if (servideId != null) {
            // Normal group, Service bound permission
            if (hasPermission(playerId, permission, null, servideId))
                return true;
            // Service bound group, Normal permission
            if (hasPermission(playerId, permission, servideId, null))
                return true;
            // Service bound group, Service bound permission
            if (hasPermission(playerId, permission, servideId, servideId))
                return true;
        }

        // Normal group, Normal permission
        return hasPermission(playerId, permission, null, null);
    }

}
