package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"containers", "onlineDurations"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "services")
public class DBService {

    @Id
    @Type(type = "text")
    private String id;

    @Type(type = "text")
    private String name;

    private Integer maxRam;

    private Float maxCpu;

    /*
     * Indicates that the service was removed
     */
    private boolean active = true;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL)
    private List<DBContainer> containers;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL)
    private List<PlayerOnlineDurations> onlineDurations;

    public DBService(String id, String name) {
        this.id = id;
        this.name = name;
    }

}
