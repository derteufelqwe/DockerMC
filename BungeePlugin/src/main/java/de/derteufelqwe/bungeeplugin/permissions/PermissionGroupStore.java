package de.derteufelqwe.bungeeplugin.permissions;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.permissions.PermissionGroup;
import org.hibernate.Session;

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


    public PermissionGroupStore() {

    }

    // Initializes the permissions
    public void init() {
        try (Session session = sessionBuilder.openSession()) {
            for (PermissionGroup group : CommonsAPI.getInstance().getAllPermissionGroups(session)) {
                // Normal permissions
                this.normalPermissions.put(group.getId(), group.getAllPermissions());

                // Service bound permissions
                this.servicePermissions.put(group.getId(), group.getAllServicePermissions());
            }
        }


    }


    /**
     * Checks if a permission is present in the permission group
     */
    public boolean hasPermission(Long groupId, String permission) {
        return this.normalPermissions.get(groupId).contains(permission);
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
