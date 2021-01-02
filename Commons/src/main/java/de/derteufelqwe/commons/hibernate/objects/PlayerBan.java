package de.derteufelqwe.commons.hibernate.objects;

import de.derteufelqwe.commons.Constants;
import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "player_bans")
public class PlayerBan {

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
    private Timestamp bannedAt = new Timestamp(System.currentTimeMillis());

    private Timestamp bannedUntil;

    @Type(type = "text")
    private String banMessage;

    @ManyToOne
    private DBPlayer unbannedBy;

    private Timestamp unbanTime;



    public PlayerBan(DBPlayer bannedPlayer, DBPlayer bannedBy, String banMessage, Timestamp bannedUntil) {
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
    public PlayerBan(DBPlayer bannedPlayer, DBPlayer bannedBy, String banMessage, long banDuration) {
        this(bannedPlayer, bannedBy, banMessage,
                new Timestamp((System.currentTimeMillis()) + banDuration));
    }

    /**
     * Ban with infinite duration
     * @param bannedPlayer
     * @param bannedBy
     * @param banMessage
     */
    public PlayerBan(DBPlayer bannedPlayer, DBPlayer bannedBy, String banMessage) {
        this(bannedPlayer, bannedBy, banMessage, Constants.BAN_PERMANENT_TIMESTAMP);
    }


    public int getDuration() {
        return (int) (this.bannedUntil.getTime() - this.bannedAt.getTime());
    }

    public boolean isPermanent() {
        return this.bannedUntil.compareTo(Constants.BAN_PERMANENT_TIMESTAMP) >= 0;
    }

    public boolean wasUnbanned() {
        return this.unbannedBy != null || this.unbanTime != null;
    }

    public boolean isActive() {
        if (this.wasUnbanned()) {
            return false;
        }

        return (System.currentTimeMillis() - this.bannedUntil.getTime()) <= 0;
    }



}
