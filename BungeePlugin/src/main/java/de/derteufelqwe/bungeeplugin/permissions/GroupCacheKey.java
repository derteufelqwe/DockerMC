package de.derteufelqwe.bungeeplugin.permissions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 *
 */
@AllArgsConstructor
@EqualsAndHashCode
class GroupCacheKey {

    /*
     * null = not service bound
     */
    @Nullable private String serviceID = null;

}
