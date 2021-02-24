package de.derteufelqwe.commons.hibernate.objects.permissions;

import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Base class for all permissions. Not present in the database. Subclasses are present in the db and inherit these fields.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Deprecated
public class PermissionBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @Type(type = "text")
    @Column(name = "permission_text")
    private String permissionText;


    public PermissionBase(String permission) {
        this.permissionText = permission;
    }

}
