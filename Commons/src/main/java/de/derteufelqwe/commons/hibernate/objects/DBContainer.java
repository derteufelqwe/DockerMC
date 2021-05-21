package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"logs", "containerStats"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "containers", indexes = {
        @Index(name = "container_NAME_IDX", columnList = "name"),
        @Index(name = "container_NODE_IDX", columnList = "node_id"),
        @Index(name = "container_SERVICE_IDX", columnList = "service_id"),
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

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Node node;

    @ManyToOne(fetch = FetchType.LAZY)
    private DBService service;

    private Timestamp startTime;

    private Timestamp stopTime;

    private Short exitcode;

    @OneToMany(mappedBy = "container", fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OrderBy("timestamp asc")
    private List<Log> logs = new ArrayList<>();


    @OneToMany(mappedBy = "container", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<ContainerStats> containerStats;

    @OneToMany(mappedBy = "container", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OrderBy("timestamp desc")
    private List<DBContainerHealth> containerHealths;


    public DBContainer(String id) {
        this.id = id;
    }

    public DBContainer(String id, String name, String image) {
        this.id = id;
        this.name = name;
        this.image = image;
    }

    public DBContainer(String id, String image, String name, String taskId, short taskSlot,
                       Node node, DBService service) {
        this.id = id;
        this.image = image;
        this.name = name;
        this.taskId = taskId;
        this.taskSlot = taskSlot;
        this.node = node;
        this.service = service;
    }

    /**
     * Returns the name how this server would be registered in BungeeCord
     */
    public String getMinecraftServerName() {
        return this.getService().getName() + "-" + this.getTaskSlot();
    }

    /**
     * Checks the latest health log if the container is currently healthy.
     */
    public boolean isHealthy() {
        if (this.containerHealths == null || this.containerHealths.size() == 0) {
            return true;
        }

        DBContainerHealth health = this.containerHealths.get(0);
        return health.isHealthy();
    }

    /**
     * Returns if the container is currently running
     * @return
     */
    public boolean isActive() {
        return this.stopTime == null;
    }

}
