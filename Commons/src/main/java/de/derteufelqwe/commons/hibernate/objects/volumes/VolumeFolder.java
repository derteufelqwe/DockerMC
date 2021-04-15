package de.derteufelqwe.commons.hibernate.objects.volumes;

import lombok.*;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"volume", "parent", "folders", "files"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "volumefolders", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
})
public class VolumeFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Type(type = "text")
    private String name;

    @OneToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Volume volume;

    @ManyToOne
    private VolumeFolder parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<VolumeFolder> folders = new ArrayList<>();

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<VolumeFile> files = new ArrayList<>();


    public VolumeFolder(String name) {
        this.name = name;
    }

}
