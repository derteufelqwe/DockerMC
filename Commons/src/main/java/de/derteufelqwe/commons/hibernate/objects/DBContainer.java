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
public class DBContainer {

    @Id
    @Type(type = "text")
    private String id;

    @Type(type = "text")
    @Column(name = "\"taskId\"")
    private String taskId;

    @Type(type = "text")
    private String name;

    @Type(type = "text")
    private String image;

    @Type(type = "text")
    private String log;

    @Column(name = "\"lastLogTimestamp\"")
    private Timestamp lastLogTimestamp;

    @ManyToOne(fetch = FetchType.EAGER)
    private Node node;

    @ManyToOne(fetch = FetchType.EAGER)
    private DBService service;

    @Column(name = "\"startTime\"")
    private Timestamp startTime;

    @Column(name = "\"stopTime\"")
    private Timestamp stopTime;

    @Column(name = "\"exitCode\"")
    private Short exitcode;

    @Column(name = "\"containerStats\"")
    @OneToMany(mappedBy = "container", cascade = CascadeType.ALL)
    private List<ContainerStats> containerStats;


    public DBContainer(String id, String image, Timestamp startTime) {
        this.id = id;
        this.image = image;
        this.startTime = startTime;
    }

    /**
     * Appends to the log
     * @param toAdd
     */
    public void appendToLog(String toAdd) {
        if (this.log == null) {
            this.log = toAdd;

        } else {
            this.log += toAdd;
        }
    }

}