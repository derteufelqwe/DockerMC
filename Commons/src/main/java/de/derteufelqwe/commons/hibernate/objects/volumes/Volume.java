package de.derteufelqwe.commons.hibernate.objects.volumes;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "volumes", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
        @Index(name = "CREATED_IDX", columnList = "created"),
})
public class Volume {

    @Id
    @Type(type = "text")
    private String id;

    @OneToOne(mappedBy = "volume")
    @Nullable
    private VolumeFolder rootFolder;

    private Timestamp created;

    private Timestamp lastMounted;

    private Timestamp lastUnmounted;


    public Volume(String id, Timestamp created) {
        this.id = id;
        this.created = created;
    }

}
