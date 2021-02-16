package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity(name = "container_logs")
public class ContainerLog {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private DBContainer container;

    private Timestamp timestamp;

    private String level;

    private String message;


    public ContainerLog(String containerId, Timestamp timestamp, String level, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
        this.container = new DBContainer(containerId);
    }

}
