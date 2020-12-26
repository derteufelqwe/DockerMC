package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString(exclude = {"onlineStats"})
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "players")
public class DBPlayer {

    @Id
    private UUID uuid;

    @Type(type = "text")
    private String name;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    private List<PlayerOnlineDurations> onlineStats;


    public DBPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

}
