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
    private String taskId;

    @Type(type = "text")
    private String name;

    @Type(type = "text")
    private String image;

    @Type(type = "text")
    private String log;

    private Timestamp lastLogTimestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    private Node node;

    @ManyToOne(fetch = FetchType.LAZY)
    private DBService service;

    private Timestamp startTime;

    private Timestamp stopTime;

    private Short exitcode;

    @OneToMany(mappedBy = "container", cascade = CascadeType.ALL)
    private List<ContainerStats> containerStats;


    public DBContainer(String id) {
        this.id = id;
    }

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
