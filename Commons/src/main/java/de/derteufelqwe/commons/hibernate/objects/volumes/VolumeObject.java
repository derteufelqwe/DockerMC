package de.derteufelqwe.commons.hibernate.objects.volumes;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
@ToString(exclude = {"data"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "volumeobjects", indexes = {
        @Index(name = "FILE_IDX", columnList = "file_id")
})
public class VolumeObject implements Serializable {

    @Id
    private long id;

    /**
     * This construct is required, so this object gets lazily fetched by the VolumeFile object.
     * See: https://thorben-janssen.com/hibernate-tip-lazy-loading-one-to-one/
     */
    @OneToOne
    @MapsId
    @JoinColumn(name = "file_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private VolumeFile file;

    private byte[] data;

}
