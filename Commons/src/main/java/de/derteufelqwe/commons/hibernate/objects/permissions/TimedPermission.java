package de.derteufelqwe.commons.hibernate.objects.permissions;

import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "timed_permissions")
public class TimedPermission extends PermissionBase {

    @Nullable
    @ManyToOne
    private DBPlayer player;

    @Nullable
    @ManyToOne
    private PermissionGroup group;

    private Timestamp timeout;


    public TimedPermission(String permission, Timestamp timeout) {
        super(permission);
        this.timeout = timeout;
    }

}
