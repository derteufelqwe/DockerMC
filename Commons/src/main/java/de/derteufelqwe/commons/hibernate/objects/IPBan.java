package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import java.net.Inet4Address;
import java.sql.Timestamp;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ip_bans")
public class IPBan {

    // ----- General Information -----

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    // ----- Ban information -----

    @Type(type = "text")
    private Inet4Address bannedIp;

    @ManyToOne
    private DBPlayer bannedBy;

    /*
     * Timestamp when the ip was banned
     */
    private Timestamp bannedAt = new Timestamp(System.currentTimeMillis() / 1000L);

    private Timestamp bannedUntil;

    @Type(type = "text")
    private String banMessage;

    @ManyToOne
    private DBPlayer unbannedBy;

    private Timestamp unbanTime;



    public IPBan(Inet4Address ip, DBPlayer bannedBy, String banMessage, Timestamp bannedUntil) {
        this.bannedIp = ip;
        this.bannedBy = bannedBy;
        this.banMessage = banMessage;
        this.bannedUntil = bannedUntil;
    }

    /**
     *
     * @param ip
     * @param bannedBy
     * @param banMessage
     * @param banDuration In seconds
     */
    public IPBan(Inet4Address ip, DBPlayer bannedBy, String banMessage, int banDuration) {
        this(ip, bannedBy, banMessage,
                new Timestamp((System.currentTimeMillis() / 1000L) + banDuration));
    }


    public int getDuration() {
        return (int) (this.bannedUntil.getTime() - this.bannedAt.getTime());
    }

    public boolean wasUnbanned() {
        return this.unbannedBy != null || this.unbanTime != null;
    }

}
