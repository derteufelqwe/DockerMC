package de.derteufelqwe.commons.hibernate.objects.economy;

import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "player_transactions")
public class PlayerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private DBPlayer from;

    @ManyToOne
    private DBPlayer to;

    private Timestamp timestamp;

    private double amount;


    public PlayerTransaction(DBPlayer from, DBPlayer to, double amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

}
