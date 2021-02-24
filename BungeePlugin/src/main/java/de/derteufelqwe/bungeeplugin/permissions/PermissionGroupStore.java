package de.derteufelqwe.bungeeplugin.permissions;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.permissions.Permission;
import de.derteufelqwe.commons.hibernate.objects.permissions.PermissionGroup;
import de.derteufelqwe.commons.misc.Pair;
import de.derteufelqwe.commons.misc.TimeoutMap;
import de.derteufelqwe.commons.misc.TimeoutPermissionStore;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.CacheEntry;
import org.cache2k.annotation.Nullable;
import org.cache2k.expiry.ExpiryPolicy;
import org.cache2k.io.AdvancedCacheLoader;
import org.cache2k.io.CacheLoader;
import org.hibernate.Session;

import java.sql.Timestamp;
import java.util.*;

/**
 * Stores the permissions a permission group has
 */
public class PermissionGroupStore {

    private final SessionBuilder sessionBuilder;

    // Data: PermissionGroup ID, Permission information, Permission information
    private Cache<Long, Cache<CacheKey, PermissionData>> cache = createMainCache();

    // Data: Permission information, Has permission
    private Cache<ResolveCacheKey, Boolean> permissionResolveCache = createPermissionResolveCache();

    public PermissionGroupStore(SessionBuilder sessionBuilder) {
        this.sessionBuilder = sessionBuilder;
    }


    /**
     * Creates the overall permission cache, which contains the caches for the individual permission group caches.
     * @return
     */
    private Cache<Long, Cache<CacheKey, PermissionData>> createMainCache() {
        return new Cache2kBuilder<Long, Cache<CacheKey, PermissionData>>() {}
                .name("PermissionGroupMainCache")
                .eternal(true)     // Entries can't expire
                .build();
    }

    /**
     * Creates a cache for a single permission group, which support permission timeouts
     * @return
     */
    private Cache<CacheKey, PermissionData> createSubCache(long groupId) {
        return new Cache2kBuilder<CacheKey, PermissionData>() {}
                .name(groupId + "-cache")
                .eternal(false)     // Entries can expire
                .expiryPolicy(new ExpiryPolicy<CacheKey, PermissionData>() {
                    @Override
                    public long calculateExpiryTime(CacheKey key, PermissionData value, long loadTime, @Nullable CacheEntry<CacheKey, PermissionData> currentEntry) {
                        if (value.getTimeout() != null)
                            return value.getTimeout().getTime();

                        return ETERNAL;  // Keys which have no timeout
                    }
                })
                .build();
    }

    /**
     * Creates the cache, which saves if a permission was queried before
     * @return
     */
    private Cache<ResolveCacheKey, Boolean> createPermissionResolveCache() {
        return new Cache2kBuilder<ResolveCacheKey, Boolean>() {}
                .name("PermissionGroupResolveCache")
                .eternal(false)
                .expiryPolicy(new ExpiryPolicy<ResolveCacheKey, Boolean>() {
                    @Override
                    public long calculateExpiryTime(ResolveCacheKey key, Boolean value, long loadTime, @Nullable CacheEntry<ResolveCacheKey, Boolean> currentEntry) {
                        // ToDo: Currently expires instantly, as the timeout doesn't get tracked.
                        return 0;
                    }
                })
                .loader(new CacheLoader<ResolveCacheKey, Boolean>() {
                    @Override
                    public Boolean load(ResolveCacheKey key) throws Exception {
                        if (key.getServiceID() == null)
                            return checkPermission(key.getGroupId(), key.getPermission(), key.getServiceID());

                        return checkPermission(key.getGroupId(), key.getPermission());
                    }
                })
                .build();
    }

    /**
     * Initializes the permission groups.
     */
    public void init() {
        cache.clear();

        try (Session session = sessionBuilder.openSession()) {
            for (PermissionGroup group : CommonsAPI.getInstance().getAllPermissionGroups(session)) {
                Cache<CacheKey, PermissionData> groupCache = this.createSubCache(group.getId());

                for (Permission perm : group.getAllPermissions()) {
                    String serviceId = perm.getService() == null ? null : perm.getService().getId();
                    CacheKey key = new CacheKey(
                            perm.getPermissionText(),
                            serviceId
                    );
                    PermissionData data = new PermissionData(
                            perm.getPermissionText(),
                            serviceId,
                            perm.getTimeout()
                    );

                    groupCache.put(key, data);
                }

                this.cache.put(group.getId(), groupCache);
            }
        }
    }


    /**
     * Checks if a permission is present in the permission group
     */
    public boolean checkPermission(Long groupId, String permission) {
        return checkPermission(groupId, permission, null);
    }

    /**
     * Additionally checks if a service bound permission exists
     */
    public boolean checkPermission(Long groupId, String permission, String serviceId) {
        Cache<CacheKey, PermissionData> groupCache = this.cache.get(groupId);
        if (groupCache == null)     // Permission group not found
            return false;

        if (groupCache.containsKey(new CacheKey(permission, serviceId)))   // Permission is found as is
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

            if (groupCache.containsKey(new CacheKey(perm, serviceId)))
                return true;
        }

        return false;
    }


    /**
     * Queries the resolvedPermission cache if a permission exists
     */
    public boolean hasPermission(Long groupId, String permission) {
        Boolean res = this.permissionResolveCache.get(new ResolveCacheKey(groupId, permission));

        return res != null && res;
    }

    /**
     * Queries the resolvedPermission cache if a permission exists
     */
    public boolean hasPermission(Long groupId, String permission, String serviceId) {
        Boolean res = this.permissionResolveCache.get(new ResolveCacheKey(groupId, permission, serviceId));

        return res != null && res;
    }


    /**
     * The key for the normal permission cache. This combines the information to identify the permission information.
     */
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class CacheKey {

        private String permission;
        private String serviceID = null;

        public CacheKey(String permission) {
            this.permission = permission;
        }

    }

    /**
     * The key for the permission resolve cache.
     */
    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class ResolveCacheKey {

        private Long groupId;
        private String permission;
        private String serviceID = null;

        public ResolveCacheKey(Long groupId, String permission) {
            this.groupId = groupId;
            this.permission = permission;
        }

    }


}
