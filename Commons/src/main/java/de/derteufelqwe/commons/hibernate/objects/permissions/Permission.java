package de.derteufelqwe.commons.hibernate.objects.permissions;

import org.jetbrains.annotations.NotNull;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@Entity(name = "permissions")
@Table(name = "permissions", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
        @Index(name = "TEXT_IDX", columnList = "permission_text"),
        @Index(name = "PLAYER_IDX", columnList = "player_uuid"),
        @Index(name = "GROUP_IDX", columnList = "group_id"),
        @Index(name = "SERVICE_IDX", columnList = "service_id"),
})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @Type(type = "text")
    @Column(name = "permission_text", nullable = false)
    private String permissionText;

    @Nullable
    @ManyToOne(optional = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DBPlayer player;

    @Nullable
    @ManyToOne(optional = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PermissionGroup group;

    // -----  Filter parameters  -----

    @ManyToOne(optional = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DBService service;

    @Column(nullable = true)
    private Timestamp timeout;


    public Permission() {

    }

    public Permission(String permission) {
        this.permissionText = permission;
    }

    public Permission(String permission, Timestamp timeout) {
        this(permission);
        this.timeout = timeout;
    }

    public Permission(String permission, Timestamp timeout, DBService dbService) {
        this(permission, timeout);
        this.service = dbService;
    }

}
