package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity(name = "node_stats")
public class NodeStats {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    private Node node;

    private Timestamp timestamp;

    @Column(name = "cpu_perc")
    private Float cpuPerc;

    @Column(name = "mem_cur")
    private Integer memCurr;


    public NodeStats(Node node, Timestamp timestamp, float cpuPerc, int memCurr) {
        this.node = node;
        this.timestamp = timestamp;
        this.cpuPerc = cpuPerc;
        this.memCurr = memCurr;
    }

}
