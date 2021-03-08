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
        @Index(name = "NAME_IDX", columnList = "name"),
})
public class Node {

    @Id
    @Type(type = "text")
    private String id;

    @Type(type = "text")
    private String name;

    private Integer maxRam;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL)
    private List<DBContainer> containers;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL)
    private List<NodeStats> nodeStats;


    public Node(String id, String name) {
        this.id = id;
        this.name = name;
    }

}
