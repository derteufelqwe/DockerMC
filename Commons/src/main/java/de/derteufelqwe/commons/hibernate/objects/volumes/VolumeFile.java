package de.derteufelqwe.commons.hibernate.objects.volumes;

import lombok.*;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
@ToString(exclude = {"parent", "data"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "volumefiles", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
        @Index(name = "HASH_IDX", columnList = "datahash"),
        @Index(name = "PARENT_IDX", columnList = "parent_id"),
})
public class VolumeFile implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Type(type = "text")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)  // Required otherwise no cascade happens
    private VolumeFolder parent;

    private Timestamp lastModified;

    /**
     * optional = false is required for lazy fetching.
     * See {@link VolumeObject} for more infos
     */
    @OneToOne(mappedBy = "file", fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    private VolumeObject data;

    private long dataHash;


    public VolumeFile(String name) {
        this.name = name;
    }

}
