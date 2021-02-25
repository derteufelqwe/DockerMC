package de.derteufelqwe.bungeeplugin.permissions;

import com.sun.istack.Nullable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.sql.Timestamp;

/**
 * Local, DB independent, permission group info
 */
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class GroupData {

    private long groupID;

    // -----  Optional parameters  -----

    @Nullable
    private String serviceID;

    @Nullable
    private Timestamp timeout;

}
