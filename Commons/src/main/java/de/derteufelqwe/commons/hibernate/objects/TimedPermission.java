package de.derteufelqwe.commons.hibernate.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "timed_permission")
@DiscriminatorValue("timedPerm")
public class TimedPermission extends Permission {

    private Timestamp timeout;

}
