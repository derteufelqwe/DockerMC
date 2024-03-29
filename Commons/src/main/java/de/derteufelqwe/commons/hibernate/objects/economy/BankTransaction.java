package de.derteufelqwe.commons.hibernate.objects.economy;

import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Transactions for a bank account.
 * Stores which players deposit / withdraw money from a bank account
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "bank_transactions")
@Table(name = "bank_transactions", indexes = {
        @Index(name = "banktrans_BANK_IDX", columnList = "bank_name"),
        @Index(name = "banktrans_PLAYER_IDX", columnList = "player_uuid"),
})
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DBPlayer player;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
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
