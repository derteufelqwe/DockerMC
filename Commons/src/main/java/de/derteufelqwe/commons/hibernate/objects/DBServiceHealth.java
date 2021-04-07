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
@ToString(exclude = {"service", "node"})
@NoArgsConstructor
@Entity
@Table(name = "service_healths", indexes = {
        @Index(name = "TASK_IDX", columnList = "taskid"),
        @Index(name = "TIMESTAMP_IDX", columnList = "createdtimestamp"),
        @Index(name = "SERVICE_IDX", columnList = "service_id"),
        @Index(name = "NODE_IDX", columnList = "node_id"),
})
public class DBServiceHealth {

    @Id
    @Type(type = "text")
    private String taskID;

    @ManyToOne
    private DBService service;

    @ManyToOne
    private Node node;

    private Timestamp createdTimestamp;

    @Type(type = "text")
    private String error;

    @Enumerated(EnumType.STRING)
    private TaskState taskState;


    public DBServiceHealth(String taskID, DBService dbService, Node node, Timestamp createdTimestamp, String error, TaskState taskState) {
        this.taskID = taskID;
        this.service = dbService;
        this.node = node;
        this.createdTimestamp = createdTimestamp;
        this.error = error;
        this.taskState = taskState;
    }


    /**
     * A mirror of the docker swarm task states and an additional state if an invalid state was found.
     * (See: https://docs.docker.com/engine/swarm/how-swarm-mode-works/swarm-task-states/)
     */
    public enum TaskState {
        NEW,
        ALLOCATED,
        PENDING,
        ASSIGNED,
        ACCEPTED,
        PREPARING,
        READY,
        STARTING,
        RUNNING,
        COMPLETE,
        SHUTDOWN,
        FAILED,
        REJECTED,
        REMOVE,
        ORPHANED,

        UNKNOWN;    // Used if an unknown state was found eg. when the engine modifies its states
    }

}
