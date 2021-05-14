package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"stacktrace"})
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "logs")
@Table(name = "logs", indexes = {
        @Index(name = "logs_TIMESTAMP_IDX", columnList = "timestamp"),
        @Index(name = "logs_CAUSEDBY_IDX", columnList = "causedby_id"),
        @Index(name = "logs_EXCEPTION_IDX", columnList = "exception_id"),
        @Index(name = "logs_CONTAINER_IDX", columnList = "container_id"),
        @Index(name = "logs_NWCONTAINER_IDX", columnList = "nwcontainer_id"),
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

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DBContainer container;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private NWContainer nwContainer;

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
    private Log causedBy;

    @OneToMany
    @JoinColumn(name = "exception_id")     // Required
    @OrderBy("timestamp asc")
    private List<Log> stacktrace = new ArrayList<>();   // Default required


    public Log(String log, Timestamp timestamp, Source source) {
        this.log = log;
        this.timestamp = timestamp;
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
        CAUSED_EXCEPTION,
        STACKTRACE,
        UNKNOWN
    }

}
