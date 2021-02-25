package de.derteufelqwe.bungeeplugin.permissions;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
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
