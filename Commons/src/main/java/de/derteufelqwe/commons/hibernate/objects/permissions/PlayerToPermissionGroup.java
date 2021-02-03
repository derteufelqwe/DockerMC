package de.derteufelqwe.commons.hibernate.objects.permissions;

import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import lombok.*;

import javax.persistence.*;

/**
 * Defines the Many to many relation table for Players and PermissionGroups
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"player", "permissionGroup"})
@Entity(name = "players_permission_groups")
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_uuid", "permissionGroup_id"})
})
public class PlayerToPermissionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private DBPlayer player;

    @ManyToOne
    private PermissionGroup permissionGroup;

}
