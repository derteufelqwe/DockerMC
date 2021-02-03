package de.derteufelqwe.bungeeplugin.permissions;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.permissions.PermissionGroup;
import de.derteufelqwe.commons.misc.Pair;
import de.derteufelqwe.commons.misc.TimeoutMap;
import de.derteufelqwe.commons.misc.TimeoutPermissionStore;
import org.hibernate.Session;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Stores the permissions a permission group has
 */
public class PermissionGroupStore {

    private final SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();

    // Data: <PermGroupId, Set<permission> >
    private Map<Long, Set<String>> normalPermissions = new HashMap<>();

    // Data: <PermGroupId, <ServiceId, Set<permissions> > >
    private Map<Long, Map<String, Set<String>>> servicePermissions = new HashMap<>();

    // Data: <PermGroupId>
    private TimeoutPermissionStore<Long> timeoutPermissions = new TimeoutPermissionStore<>(60000);


    public PermissionGroupStore() {
        this.timeoutPermissions.start();
    }

    /**
     * Initializes the permissions
     */
    public void init() {
        try (Session session = sessionBuilder.openSession()) {
            for (PermissionGroup group : CommonsAPI.getInstance().getAllPermissionGroups(session)) {
                // Normal permissions
                this.normalPermissions.put(group.getId(), group.getAllPermissions());

                // Service bound permissions
                this.servicePermissions.put(group.getId(), group.getAllServicePermissions());

                // Timeout permissions
                for (Pair<String, Timestamp> perm : group.getAllTimedPermissions()) {
                    this.timeoutPermissions.add(group.getId(), perm.getA(), perm.getB());
                }
            }
        }


    }


    /**
     * Checks if a permission is present in the permission group
     */
    public boolean hasPermission(Long groupId, String permission) {
        return this.normalPermissions.get(groupId).contains(permission) || this.timeoutPermissions.contains(groupId, permission);
    }

    /**
     * Additionally checks if a service bound permission exists
     */
    public boolean hasPermission(Long groupId, String permission, String serviceId) {
        if (this.hasPermission(groupId, permission))
            return true;

        Set<String> servicePerms = this.servicePermissions.get(groupId).get(serviceId);
        return servicePerms != null && servicePerms.contains(permission);
    }

}
