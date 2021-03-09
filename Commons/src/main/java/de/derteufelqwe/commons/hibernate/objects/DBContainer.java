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
@ToString(exclude = {"log", "containerStats"})
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "containers")
@Table(name = "containers", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
        @Index(name = "NAME_IDX", columnList = "name"),
        @Index(name = "NODE_IDX", columnList = "node_id"),
        @Index(name = "SERVICE_IDX", columnList = "service_id"),
})
public class DBContainer {

    /*
     * Exit codes:
     *  0  : OK
     *  125: docker daemon error
     *  126: command can't be invoked (inside of the container)
     *  127: command cannot be found
     *  130: container terminated by ctrl-c
     *  137: container received SIGKILL
     *  143: container received SIGTERM
     * Custom ones
     *  51 : Container already deleted before its data could be transferred into the database (stop time will be invalid)
     */

    @Id
    @Type(type = "text")
    private String id;

    @Type(type = "text")
    private String taskId;

    @Type(type = "text")
    private String name;

    private short taskSlot;

    @Type(type = "text")
    private String image;

    private String ip;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<ContainerStats> containerStats;


    public DBContainer(String id) {
        this.id = id;
    }

    public DBContainer(String id, String image, Timestamp startTime, String name, String taskId, String ip, short taskSlot,
                       Node node, DBService service) {
        this.id = id;
        this.image = image;
        this.startTime = startTime;
        this.name = name;
        this.taskId = taskId;
        this.ip = ip;
        this.taskSlot = taskSlot;
        this.node = node;
        this.service = service;
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

    /**
     * Returns the name how this server would be registered in BungeeCord
     */
    public String getMinecraftServerName() {
        return this.getService().getName() + "-" + this.getTaskSlot();
    }

}
