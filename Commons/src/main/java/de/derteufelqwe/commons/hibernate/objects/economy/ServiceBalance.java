package de.derteufelqwe.commons.hibernate.objects.economy;

import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

/**
 * Stores the players balance on a certain service. The money differs across different services
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "service_balance")
@Table(name = "service_balance", indexes = {
        @Index(name = "servicebalance_PLAYER_IDX", columnList = "player_uuid"),
        @Index(name = "servicebalance_SERVICE_IDX", columnList = "service_id"),
})
public class ServiceBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DBPlayer player;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DBService service;

    private double moneyBalance = 0;


    public ServiceBalance(DBPlayer dbPlayer, DBService dbService) {
        this.player = dbPlayer;
        this.service = dbService;
    }

}
