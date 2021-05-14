package de.derteufelqwe.commons.hibernate.objects.economy;

import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Stores all general, server wide, transactions between players
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "player_transactions")
@Table(name = "player_transactions", indexes = {
        @Index(name = "playertrans_FROM_IDX", columnList = "from_uuid"),
        @Index(name = "playertrans_TO_IDX", columnList = "to_uuid"),
})
public class PlayerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DBPlayer from;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
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
