package de.derteufelqwe.commons.hibernate.objects.permissions;

import com.sun.istack.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Permission> permissions = new ArrayList<>();


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
    public Set<Permission> getAllPermissions() {
        Set<Permission> perms = new HashSet<>();
        if (this.parent != null) {
            perms = this.parent.getAllPermissions();
        }

        if (this.permissions != null) {
            perms.addAll(this.permissions);
        }

        return perms;
    }

}
