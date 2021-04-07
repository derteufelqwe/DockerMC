package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"healths", "containers", "logins"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "services", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
        @Index(name = "NAME_IDX", columnList = "name"),
})
public class DBService {

    @Id
    @Type(type = "text")
    private String id;

    @Type(type = "text")
    private String name;

    private Integer maxRam;

    private Float maxCpu;

    private int replicas;

    @Type(type = "text")
    private String type;

    /*
     * Indicates that the service was removed. false = removed
     */
    private boolean active = true;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OrderBy("createdTimestamp desc")
    private List<DBServiceHealth> healths;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<DBContainer> containers;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PlayerLogin> logins;

    public DBService(String id, String name, int maxRam, float maxCpu, String type, int replicas) {
        this.id = id;
        this.name = name;
        this.maxRam = maxRam;
        this.maxCpu = maxCpu;
        this.type = type;
        this.replicas = replicas;
    }

    // --- Custom query parameters ---

    /**
     * Number of containers that are running for this service
     */
    @Formula("(SELECT COUNT(*) FROM containers AS c WHERE c.service_id = id AND c.stoptime IS NULL AND c.starttime IS NOT NULL)")
    @Basic(fetch = FetchType.LAZY)
    private int runningContainersCount;


    public boolean isHealthy() {
        return this.replicas == runningContainersCount;
    }


}
