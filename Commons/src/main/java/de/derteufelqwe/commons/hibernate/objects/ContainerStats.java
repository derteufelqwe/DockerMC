package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity(name = "container_stats")
@Table(name = "container_stats", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
        @Index(name = "CONTAINER_IDX", columnList = "container_id"),
        @Index(name = "TIMESTAMP_IDX", columnList = "timestamp"),
})
public class ContainerStats {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private DBContainer container;

    private Timestamp timestamp;

    private Float cpuPerc;

    private Float memCurr;


    public ContainerStats(DBContainer container, Timestamp timestamp, float cpuPerc, float memCurr) {
        this.container = container;
        this.timestamp = timestamp;
        this.cpuPerc = cpuPerc;
        this.memCurr = memCurr;
    }

}
