package de.derteufelqwe.commons.hibernate.objects.permissions;

import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "service_permissions")
public class ServicePermission extends PermissionBase {

    @Nullable
    @ManyToOne
    private DBPlayer player;

    @Nullable
    @ManyToOne
    private PermissionGroup group;

    @ManyToOne
    private DBService service;

}
