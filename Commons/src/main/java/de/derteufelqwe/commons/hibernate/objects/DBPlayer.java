package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString(exclude = {"onlineStats", "gottenBans", "executedBans"})
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "players")
public class DBPlayer {

    // ----- General Information -----

    @Id
    private UUID uuid;

    @Type(type = "text")
    private String name;

    // ----- Stats -----

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    @Column(name = "\"onlineStats\"")
    private List<PlayerOnlineDurations> onlineStats;

    private Timestamp lastOnline;

    // ----- Ban information -----

    @OneToMany(mappedBy = "bannedPlayer", cascade = CascadeType.ALL)
    @Column(name = "\"gottenBans\"")
    private List<PlayerBans> gottenBans;

    @OneToMany(mappedBy = "bannedBy", cascade = CascadeType.ALL)
    @Column(name = "\"executedBans\"")
    private List<PlayerBans> executedBans;


    public DBPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

}
