package de.derteufelqwe.commons.hibernate.objects;

import de.derteufelqwe.commons.exceptions.DatabaseException;
import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "player_logins")
public class PlayerLogin {

    // ----- General Information -----

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // ----- Login information -----

    @ManyToOne
    private DBPlayer player;

    private Timestamp joinTime = new Timestamp(System.currentTimeMillis());

    private Timestamp leaveTime;


    public PlayerLogin(DBPlayer player) {
        this.player = player;
    }


    public long getOnlineDuration() throws DatabaseException {
        if (this.leaveTime == null) {
            throw new DatabaseException("Player " + this.player.getUuid() + " hasn't left yet.");
        }

        return this.leaveTime.getTime() - this.joinTime.getTime();
    }


}
