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
@ToString(exclude = {"volume", "parent", "data"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "volumefiles", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
        @Index(name = "HASH_IDX", columnList = "datahash"),
})
public class VolumeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Type(type = "text")
    private String name;

    @ManyToOne
    private Volume volume;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)  // Required otherwise no cascade happens
    private VolumeFolder parent;

    private Timestamp lastModified;

    @Basic(fetch = FetchType.LAZY)
    private byte[] data;

    private byte[] dataHash;


    public VolumeFile(String name) {
        this.name = name;
    }

}
