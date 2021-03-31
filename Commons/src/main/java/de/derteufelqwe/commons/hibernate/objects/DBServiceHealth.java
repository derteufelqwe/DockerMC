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
@ToString(exclude = {"service"})
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "service_healths")
@Table(name = "service_healths", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
        @Index(name = "TIMESTAMP_IDX", columnList = "timestamp"),
})
public class DBServiceHealth {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DBService service;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Node node;

    @Type(type = "text")
    private String taskID;

    private Timestamp timestamp;

    @Type(type = "text")
    private String error;

    @Type(type = "text")
    private String taskState;


    public DBServiceHealth(String taskID, DBService dbService, Node node, Timestamp timestamp, String error, String taskState) {
        this.taskID = taskID;
        this.service = dbService;
        this.node = node;
        this.timestamp = timestamp;
        this.error = error;
        this.taskState = taskState;
    }

}
