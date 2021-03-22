package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@ToString(exclude = {"container"})
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "container_healths")
@Table(name = "container_healths", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
        @Index(name = "CONTAINER_ID", columnList = "container_id"),
        @Index(name = "TIMESTAMP_IDX", columnList = "timestamp"),
})
public class DBContainerHealth {

    // ----- General Information -----

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private DBContainer container;

    private Timestamp timestamp;

    @Type(type = "text")
    private String message;

    private short exitCode;


    public DBContainerHealth(Timestamp timestamp, DBContainer container, String message, int exitCode) {
        this.timestamp = timestamp;
        this.container = container;
    }


    public boolean isHealthy() {
        return this.exitCode == 0;
    }

}
