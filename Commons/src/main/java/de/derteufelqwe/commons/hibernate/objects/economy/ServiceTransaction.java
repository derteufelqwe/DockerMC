package de.derteufelqwe.commons.hibernate.objects.economy;

import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Stores service bound transactions between players
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "service_transactions")
@Table(name = "service_transactions", indexes = {
        @Index(name = "servicetrans_FROM_IDX", columnList = "from_uuid"),
        @Index(name = "servicetrans_TO_IDX", columnList = "to_uuid"),
        @Index(name = "servicetrans_SERVICE_IDX", columnList = "service_id"),
})
public class ServiceTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    private DBService service;

    @ManyToOne
    private DBPlayer from;

    @ManyToOne
    private DBPlayer to;

    private Timestamp timestamp;

    private double amount;


    public ServiceTransaction(DBService service, DBPlayer from, DBPlayer to, double amount) {
        this.service = service;
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

}
