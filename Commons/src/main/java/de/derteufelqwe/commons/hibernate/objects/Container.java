package de.derteufelqwe.commons.hibernate.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Container {

    @Id
    private String containerId;

    private String containerName;

    /**
     * Name of the server like LobbyServer or Minigame
     */
    private String serverName;

    private String nodeId;

    /**
     * Contains the logs of the container
     */
    @Type(type = "text")
    private String log;

    private String image;

    private Timestamp stopTimestamp;

    private short exitCode = Short.MIN_VALUE;

    private Timestamp lastLogTimestamp;


    public Container(String containerId, String image, String log, Timestamp stopTimestamp, Timestamp lastLogTimestamp) {
        this.containerId = containerId;
        this.image = image;
        this.log = log;
        this.stopTimestamp = stopTimestamp;
        this.lastLogTimestamp = lastLogTimestamp;
    }

}
