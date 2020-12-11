package de.derteufelqwe.commons.hibernate.objects;

import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity(name = "container_stats")
public class ContainerStats {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    private Container container;

    private Integer timestamp;

    @Column(name = "cpu_perc")
    private Float cpuPerc;

    @Column(name = "mem_curr")
    private Float memCurr;

}
