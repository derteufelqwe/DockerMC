package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"containers", "nodeStats", "serviceHealths"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "nodes", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
})
public class Node {

    @Id
    @Type(type = "text")
    private String id;

    @Type(type = "text")
    private String name;    // Hostname of the hosting server

    private Timestamp joinTime;

    /**
     * Leave time might not be accurate if the node leaves when the NodeWatcher is not online during this
     */
    private Timestamp leaveTime;

    @Type(type = "text")
    private String ip;

    private boolean manager;

    private Integer maxRAM;

    private Float availableCPUs;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<DBContainer> containers;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<NodeStats> nodeStats;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<DBServiceHealth> serviceHealths;


    public Node(String id, String name, Timestamp joinTime, String ip, boolean manager, int maxRAM, float availableCPUs) {
        this.id = id;
        this.name = name;
        this.joinTime = joinTime;
        this.ip = ip;
        this.manager = manager;
        this.maxRAM = maxRAM;
        this.availableCPUs = availableCPUs;
    }

}
