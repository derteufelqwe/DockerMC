package de.derteufelqwe.commons.hibernate.objects;

import de.derteufelqwe.commons.exceptions.DatabaseException;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Stores when players connect / leave servers
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "player_logins", indexes = {
        @Index(name = "ID_IDX", columnList = "id"),
        @Index(name = "PLAYER_IDX", columnList = "player_uuid"),
        @Index(name = "SERVICE_IDX", columnList = "service_id"),
        @Index(name = "TIMESTAMP_IDX", columnList = "jointime"),
})
public class PlayerLogin {

    // ----- General Information -----

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // ----- Login information -----

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DBPlayer player;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DBService service;

    private Timestamp joinTime = new Timestamp(System.currentTimeMillis());

    private Timestamp leaveTime;


    public PlayerLogin(DBPlayer player, DBService service) {
        this.player = player;
        this.service = service;
    }


    public long getOnlineDuration() throws DatabaseException {
        if (this.leaveTime == null) {
            throw new DatabaseException("Player " + this.player.getUuid() + " hasn't left yet.");
        }

        return this.leaveTime.getTime() - this.joinTime.getTime();
    }


}
