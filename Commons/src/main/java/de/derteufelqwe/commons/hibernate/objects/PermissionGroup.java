package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.List;

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

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "group_id")
    private List<Permission> permissions;

    @ManyToOne
    private PermissionGroup parent;


    public PermissionGroup(String name) {
        this.name = name;
    }

}
