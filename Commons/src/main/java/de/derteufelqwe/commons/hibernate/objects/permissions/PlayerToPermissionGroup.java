package de.derteufelqwe.commons.hibernate.objects.permissions;

import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Defines the Many to many relation table for Players and PermissionGroups
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"player", "permissionGroup"})
@Entity
@Table(name = "players_permission_groups", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
        @Index(name = "TIMEOUT_IDX", columnList = "timeout"),
        @Index(name = "PLAYER_IDX", columnList = "player_uuid"),
        @Index(name = "PERMISSIONGROUP_IDX", columnList = "permissiongroup_id"),
        @Index(name = "SERVICE_IDX", columnList = "service_id"),
})
public class PlayerToPermissionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DBPlayer player;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PermissionGroup permissionGroup;

    // -----  Additional properties  -----

    @Column(nullable = true)
    private Timestamp timeout;

    @ManyToOne(optional = true)
    private DBService service;


    public PlayerToPermissionGroup(DBPlayer player, PermissionGroup group) {
        this.player = player;
        this.permissionGroup = group;
    }

}
