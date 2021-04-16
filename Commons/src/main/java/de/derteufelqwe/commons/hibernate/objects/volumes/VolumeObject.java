package de.derteufelqwe.commons.hibernate.objects.volumes;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "volumeobjects", indexes = {
        @Index(name = "FILE_IDX", columnList = "file_id")
})
public class VolumeObject {

    @Id
    @OneToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private VolumeFile file;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "data")
    private byte[] data;

}
