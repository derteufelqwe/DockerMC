package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "player_bans")
public class PlayerBans {

    // ----- General Information -----

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    // ----- Ban information -----

    @ManyToOne
    private DBPlayer bannedPlayer;

    @ManyToOne
    private DBPlayer bannedBy;

    /*
     * Timestamp when the player was banned
     */
    private Timestamp bannedAt = new Timestamp(System.currentTimeMillis() / 1000L);

    private Timestamp bannedUntil;

    @Type(type = "text")
    private String banMessage;


    public PlayerBans(DBPlayer bannedPlayer, DBPlayer bannedBy, String banMessage, Timestamp bannedUntil) {
        this.bannedPlayer = bannedPlayer;
        this.bannedBy = bannedBy;
        this.banMessage = banMessage;
        this.bannedUntil = bannedUntil;
    }

    /**
     *
     * @param bannedPlayer
     * @param bannedBy
     * @param banMessage
     * @param banDuration In seconds
     */
    public PlayerBans(DBPlayer bannedPlayer, DBPlayer bannedBy, String banMessage, int banDuration) {
        this(bannedPlayer, bannedBy, banMessage,
                new Timestamp((System.currentTimeMillis() / 1000L) + banDuration));
    }


    public int getDuration() {
        return (int) (this.bannedUntil.getTime() - this.bannedAt.getTime());
    }

}
