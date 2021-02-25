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
@Entity(name = "players_permission_groups")
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
