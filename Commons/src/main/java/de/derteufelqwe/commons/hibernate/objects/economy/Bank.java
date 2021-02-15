package de.derteufelqwe.commons.hibernate.objects.economy;

import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Table for money banks. Banks are available across all servers
 */
@Getter
@Setter
@ToString(exclude = {"transactions"})
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "banks")
public class Bank {

    @Id
    private String name;

    private double moneyBalance = 0;

    @ManyToOne
    private DBPlayer owner;

    @OneToMany(mappedBy = "bank")
    private List<PlayerToBank> members = new ArrayList<>();

    @OrderBy("id desc")
    @OneToMany(mappedBy = "bank", cascade = CascadeType.ALL)
    private List<BankTransaction> transactions = new ArrayList<>();


    public Bank(String name, DBPlayer owner) {
        this.name = name;
        this.owner = owner;
    }


    public boolean hasMember(DBPlayer player) {
        return this.members.stream()
                .filter(p -> p.getPlayer().getUuid().equals(player.getUuid()))
                .count() > 0;
    }

}
