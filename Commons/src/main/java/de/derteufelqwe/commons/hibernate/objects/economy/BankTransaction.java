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
@Entity(name = "bank_transactions")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private DBPlayer player;

    @ManyToOne
    private Bank bank;

    private Timestamp timestamp;

    private double amount;

    /**
     * true: deposit
     * false: withdraw
     */
    private boolean deposit;


    public BankTransaction(DBPlayer player, Bank bank, double amount, boolean deposit) {
        this.player = player;
        this.bank = bank;
        this.amount = amount;
        this.deposit = deposit;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

}
