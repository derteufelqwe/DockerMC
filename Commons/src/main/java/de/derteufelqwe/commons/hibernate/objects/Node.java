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
public class Node {

    @Id
    @Type(type = "text")
    private String id;

    @Type(type = "text")
    private String name;

    @Column(name = "\"maxRam\"")
    private Integer maxRam;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL)
    private List<DBContainer> containers;

    @Column(name = "\"nodeStats\"")
    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL)
    private List<NodeStats> nodeStats;


    public Node(String id, String name) {
        this.id = id;
        this.name = name;
    }

}
