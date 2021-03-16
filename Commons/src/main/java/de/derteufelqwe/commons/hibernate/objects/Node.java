package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"containers", "nodeStats"})
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "nodes")
@Table(name = "nodes", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
})
public class Node {

    @Id
    @Type(type = "text")
    private String id;

    private Integer maxRam;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL)
    private List<DBContainer> containers;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL)
    private List<NodeStats> nodeStats;


    public Node(String id, int maxRam) {
        this.id = id;
        this.maxRam = maxRam;
    }

}
