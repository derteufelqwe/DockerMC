package de.derteufelqwe.commons.hibernate.objects.volumes;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"folders", "files"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "volumes", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
})
public class Volume {

    @Id
    @Type(type = "text")
    private String id;

    private Timestamp created;

    @OneToMany(mappedBy = "volume", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<VolumeFolder> folders = new ArrayList<>();

    @OneToMany(mappedBy = "volume", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<VolumeFile> files = new ArrayList<>();



    public Volume(String id, Timestamp created) {
        this.id = id;
        this.created = created;
    }

}
