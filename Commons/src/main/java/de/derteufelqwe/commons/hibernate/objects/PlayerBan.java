package de.derteufelqwe.commons.hibernate.objects;

import de.derteufelqwe.commons.Constants;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "player_bans")
@Table(name = "player_bans", indexes = {
        @Index(name = "pban_BANNED_PLAYER_IDX", columnList = "bannedplayer_uuid"),
        @Index(name = "pban_BANNED_BY_IDX", columnList = "bannedby_uuid"),
        @Index(name = "pban_UNBANNED_BY_IDX", columnList = "unbannedby_uuid"),
        @Index(name = "pban_BANNED_UNTIL_IDX", columnList = "banneduntil"),
})
public class PlayerBan {

    // ----- General Information -----

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // ----- Ban information -----

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DBPlayer bannedPlayer;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DBPlayer bannedBy;

    /*
     * Timestamp when the player was banned
     */
    private Timestamp bannedAt = new Timestamp(System.currentTimeMillis());

    private Timestamp bannedUntil;

    @Type(type = "text")
    private String banMessage;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
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


/*
-- auto-generated definition
create table player_bans
(
    id                bigint not null
        constraint player_bans_pkey
            primary key,
    banmessage        text,
    bannedat          timestamp,
    banneduntil       timestamp,
    unbantime         timestamp,
    bannedby_uuid     uuid
        constraint fk7dgd1vah0l28ssk4x5ufcl277
            references players,
    bannedplayer_uuid uuid
        constraint fkqx0hx67p0gth1r9qmh1p4oh7v
            references players,
    unbannedby_uuid   uuid
        constraint fk25jwsu0kx2rp6rrsdextffw7i
            references players
);

alter table player_bans
    owner to admin;


 */