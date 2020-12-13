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
public class ContainerStats {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    private DBContainer container;

    private Timestamp timestamp;

    @Column(name = "cpu_perc")
    private Float cpuPerc;

    @Column(name = "mem_cur")
    private Float memCurr;


    public ContainerStats(DBContainer container, Timestamp timestamp, float cpuPerc, float memCurr) {
        this.container = container;
        this.timestamp = timestamp;
        this.cpuPerc = cpuPerc;
        this.memCurr = memCurr;
    }

}
