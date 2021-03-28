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

    /**
     * Note: Postgresql only supports microseconds, therefore the last digit of the TS gets rounded
     */
    private Timestamp timestamp;

    @Type(type = "text")
    private String container;

    @Enumerated(EnumType.ORDINAL)
    private Source source;

    // -----  Exception infos  -----

    @Enumerated(EnumType.ORDINAL)
    private MsgType type = MsgType.LOG;     // Default required

    @Type(type = "text")
    private String exceptionType;

    @Type(type = "text")
    private String exceptionMessage;

    @OneToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Log causedBy;

    @OneToMany
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Log> stacktrace = new ArrayList<>();   // Default required


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

    public enum MsgType {
        LOG,
        EXCEPTION,
        STACKTRACE,
        UNKNOWN
    }

}
