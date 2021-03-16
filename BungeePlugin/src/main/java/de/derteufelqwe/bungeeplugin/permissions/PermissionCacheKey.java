package de.derteufelqwe.bungeeplugin.permissions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * The key for the normal permission cache. This combines the information to identify the permission information.
 */
@AllArgsConstructor
@EqualsAndHashCode
class PermissionCacheKey {

    @NotNull
    private String permission;
    /*
     * null = not service bound
     */
    @Nullable
    private String serviceID = null;

    public PermissionCacheKey(String permission) {
        this.permission = permission;
    }

}
