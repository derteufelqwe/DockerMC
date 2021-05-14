package de.derteufelqwe.commons.hibernate.objects;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Map;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "notifications")
@TypeDefs({
        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
@Table(name = "notifications", indexes = {
        @Index(name = "notif_TYPE_IDX", columnList = "type"),
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Type(type = "text")
    private String type;

    @Type(type = "text")
    private String message;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    private boolean read = false;

    private Timestamp timestamp;


    public Notification(String type) {
        this.type = type;
    }

    public Notification(String type, String message, Map<String, Object> data) {
        this.type = type;
        this.message = message;
        this.data = data;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

}
