package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"containerStats"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "containers")
public class Container {

    @Id
    @Type(type = "text")
    private String id;

    @Type(type = "text")
    private String name;

    @Type(type = "text")
    private String image;

    @Type(type = "text")
    private String log;

    @ManyToOne(fetch = FetchType.EAGER)
    private Node node;

    @Column(name = "\"startTime\"")
    private Timestamp startTime;

    @Column(name = "\"stopTime\"")
    private Timestamp stopTime;

    @Column(name = "\"exitCode\"")
    private Short exitcode;

    @Column(name = "\"maxRam\"")
    private Integer maxRam;

    @Column(name = "\"containerStats\"")
    @OneToMany(mappedBy = "container", cascade = CascadeType.ALL)
    private List<ContainerStats> containerStats;

    public Container(String id, String image, Timestamp startTime) {
        this.id = id;
        this.image = image;
        this.startTime = startTime;
    }

}
