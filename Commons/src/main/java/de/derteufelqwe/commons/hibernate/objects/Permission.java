package de.derteufelqwe.commons.hibernate.objects;

import jakarta.validation.Constraint;
import lombok.*;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "permissions")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("normalPerm")
public class Permission {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    @Type(type = "text")
    private String permission;

//    @ManyToOne
//    private PermissionGroup group;

    public Permission(String permission) {
        this.permission = permission;
    }

}
