package de.derteufelqwe.commons.hibernate.objects.volumes;

import lombok.*;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
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
        @Index(name = "DATA_IDX", columnList = "data_id"),
})
public class VolumeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Type(type = "text")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)  // Required otherwise no cascade happens
    private VolumeFolder parent;

    private Timestamp lastModified;

    @OneToOne(mappedBy = "file")
    private VolumeObject data;

    @Basic(fetch = FetchType.LAZY)
    private byte[] dataHash;


    public VolumeFile(String name) {
        this.name = name;
    }

}
