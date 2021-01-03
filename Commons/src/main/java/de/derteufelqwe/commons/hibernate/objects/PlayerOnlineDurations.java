package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity(name = "player_online_durations")
public class PlayerOnlineDurations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private DBPlayer player;

    @ManyToOne
    private DBService service;

    /*
     * Amount of seconds a player was online on that server
     */
    private int duration = 0;


    public PlayerOnlineDurations(DBPlayer player, DBService service) {
        this.player = player;
        this.service = service;
    }

}
