package de.derteufelqwe.bungeeplugin.permissions;

import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.permissions.Permission;
import de.derteufelqwe.commons.hibernate.objects.permissions.PermissionGroup;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.CacheEntry;
import org.cache2k.expiry.ExpiryPolicy;
import org.hibernate.Session;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the permissions a permission group has
 */
public class PermissionGroupStore {

    private final SessionBuilder sessionBuilder;

    // Data: PermissionGroup ID, Permission access infos, Detailed Permission information
    private Map<Long, Cache<PermissionCacheKey, PermissionData>> cache = new HashMap<>();

    public PermissionGroupStore(SessionBuilder sessionBuilder) {
        this.sessionBuilder = sessionBuilder;
    }


    /**
     * Creates a cache for a single permission group, which support permission timeouts
     *
     * @return
     */
    private Cache<PermissionCacheKey, PermissionData> createSubCache(long groupId) {
        return new Cache2kBuilder<PermissionCacheKey, PermissionData>() {}
                .name("group-" + groupId)
                .eternal(false)     // Entries can expire
                .expiryPolicy(new ExpiryPolicy<PermissionCacheKey, PermissionData>() {
                    @Override
                    public long calculateExpiryTime(PermissionCacheKey key, PermissionData value, long loadTime, CacheEntry<PermissionCacheKey, PermissionData> currentEntry) {
                        if (value.getTimeout() != null)
                            return value.getTimeout().getTime();

                        return ETERNAL;  // Keys which have no timeout
                    }
                })
                .build();
    }

    /**
     * Initializes the permission groups.
     */
    public void init() {
        this.reset();

        try (Session session = sessionBuilder.openSession()) {
            for (PermissionGroup group : CommonsAPI.getInstance().getAllPermissionGroups(session)) {
                Cache<PermissionCacheKey, PermissionData> groupCache = this.createSubCache(group.getId());

                for (Permission perm : group.getAllPermissions()) {
                    String serviceId = perm.getService() == null ? null : perm.getService().getId();
                    PermissionCacheKey key = new PermissionCacheKey(
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
     * Resets the whole cache
     */
    public void reset() {
        for (Map.Entry<Long, Cache<PermissionCacheKey, PermissionData>> entry : cache.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().clearAndClose();
            }
        }

        cache.clear();
    }


    /**
     * Queries the resolvedPermission cache if a permission exists
     */
    public boolean hasPermission(Long groupId, String permission) {
        return hasPermission(groupId, permission, null);
    }

    /**
     * Queries the resolvedPermission cache if a permission exists
     */
    public boolean hasPermission(Long groupId, String permission, String serviceId) {
        Cache<PermissionCacheKey, PermissionData> groupCache = this.cache.get(groupId);
        if (groupCache == null)     // Permission group not found
            return false;

        if (groupCache.containsKey(new PermissionCacheKey(permission, serviceId)))   // Permission is found as is
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

            if (groupCache.containsKey(new PermissionCacheKey(perm, serviceId)))
                return true;
        }

        return false;
    }


}
