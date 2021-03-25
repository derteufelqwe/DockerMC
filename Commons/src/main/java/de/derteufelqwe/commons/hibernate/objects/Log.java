package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@ToString()
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "logs")
@Table(name = "logs", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
})
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Type(type = "text")
    private String log;

    private Timestamp timestamp;

    @Type(type = "text")
    private String container;

    @Enumerated(EnumType.ORDINAL)
    private Source source;


    public Log(String log, Timestamp timestamp, String container, Source source) {
        this.log = log;
        this.timestamp = timestamp;
        this.container = container;
        this.source = source;
    }


    public enum Source {
        STDOUT,
        STDERR,
        UNKNOWN,
    }

}
