package de.derteufelqwe.commons.hibernate.objects.permissions;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"permissions"})
@Entity(name = "permission_groups")
public class PermissionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Type(type = "text")
    @Column(nullable = false)
    private String name;

    @Type(type = "text")
    private String prefix;

    @ManyToOne
    private PermissionGroup parent;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id")
    private List<Permission> permissions;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "group_id")
    private List<ServicePermission> servicePermissions;


    public PermissionGroup(String name) {
        this.name = name;
    }


    /**
     * Returns the permissions including the permissions of the parents
     */
    public Set<String> getAllPermissions() {
        Set<String> perms = new HashSet<>();
        if (this.parent != null) {
            perms = this.parent.getAllPermissions();
        }

        if (this.permissions != null) {
            perms.addAll(
                this.permissions.stream()
                    .map(PermissionBase::getPermissionText)
                    .collect(Collectors.toSet())
            );
        }

        return perms;
    }

    /**
     * Returns a map, which maps the serviceId to a set of their permissions
     */
    public Map<String, Set<String>> getAllServicePermissions() {
        Map<String, Set<String>> perms = new HashMap<>();
        if (this.parent != null) {
            perms = this.parent.getAllServicePermissions();
        }

        if (this.servicePermissions != null) {
            for (ServicePermission p : this.servicePermissions) {
                String serviceId = p.getService().getId();
                perms.computeIfAbsent(serviceId, k -> new HashSet<>()); // Add set if not existing

                perms.get(serviceId).add(p.getPermissionText());
            }
        }

        return perms;
    }

}
