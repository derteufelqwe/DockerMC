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
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private Node node;

    private Timestamp timestamp;

    private Float cpuPerc;

    private Integer memCurr;


    public NodeStats(Node node, Timestamp timestamp, float cpuPerc, int memCurr) {
        this.node = node;
        this.timestamp = timestamp;
        this.cpuPerc = cpuPerc;
        this.memCurr = memCurr;
    }

}
