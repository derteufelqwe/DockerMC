package de.derteufelqwe.commons.hibernate.objects.permissions;

import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import jakarta.validation.Constraint;
import lombok.*;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.annotations.Type;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@Entity(name = "permissions")
public class Permission extends PermissionBase {

    @Nullable
    @ManyToOne
    private DBPlayer player;

    @Nullable
    @ManyToOne
    private PermissionGroup group;


    public Permission() {
        super();
    }

    public Permission(String permission) {
        super(permission);
    }

}
