package de.derteufelqwe.commons.hibernate.objects.economy;

import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
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


    public Bank(String name, DBPlayer owner) {
        this.name = name;
        this.owner = owner;
    }

}
