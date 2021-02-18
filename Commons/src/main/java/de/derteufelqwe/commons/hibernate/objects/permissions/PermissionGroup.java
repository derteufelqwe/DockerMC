package de.derteufelqwe.commons.hibernate.objects.permissions;

import com.sun.istack.NotNull;
import de.derteufelqwe.commons.misc.Pair;
import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;
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

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Permission> permissions;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<ServicePermission> servicePermissions;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<TimedPermission> timedPermissions;


    public PermissionGroup(String name) {
        this.name = name;
    }

    // -----  Custom getters  -----

    @NotNull
    public String getPrefix() {
        return this.prefix != null ? this.prefix : "";
    }

    // -----  Other methods  -----

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

    /**
     * Returns all timed permissions including the permissions of the parents
     * @return
     */
    public Set<Pair<String, Timestamp>> getAllTimedPermissions() {
        Set<Pair<String, Timestamp>> perms = new HashSet<>();
        if (this.parent != null) {
            perms = this.parent.getAllTimedPermissions();
        }

        if (this.timedPermissions != null) {
            perms.addAll(
                    this.timedPermissions.stream()
                            .map(p -> new Pair<String, Timestamp>(p.getPermissionText(), p.getTimeout()))
                            .collect(Collectors.toSet())
            );
        }

        return perms;
    }

}
