package de.derteufelqwe.bungeeplugin.permissions;

import org.jetbrains.annotations.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.sql.Timestamp;

/**
 * Local, DB independent, permission information
 */
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class PermissionData {

    private String permission;

    // -----  Optional parameters  -----

    @Nullable
    private String serviceID;

    @Nullable
    private Timestamp timeout;

}
