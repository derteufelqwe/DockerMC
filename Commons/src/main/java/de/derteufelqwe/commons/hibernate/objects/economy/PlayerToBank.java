package de.derteufelqwe.commons.hibernate.objects.economy;

import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import lombok.*;

import javax.persistence.*;

/**
 * Many-To-Many table to connect players and banks
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "player_banks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_uuid", "bank_name"})
}, indexes = {
        @Index(name = "playertobank_PLAYER_IDX", columnList = "player_uuid"),
        @Index(name = "playertobank_BANK_IDX", columnList = "bank_name"),
})

public class PlayerToBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private DBPlayer player;

    @ManyToOne
    private Bank bank;


    public PlayerToBank(DBPlayer player, Bank bank) {
        this.player = player;
        this.bank = bank;
    }

}
