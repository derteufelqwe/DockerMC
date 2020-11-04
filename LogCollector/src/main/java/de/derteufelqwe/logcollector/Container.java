package de.derteufelqwe.logcollector;

import com.github.dockerjava.api.model.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKey;
import java.sql.Timestamp;
import java.util.Map;

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


    public Container(Event event, String log, Timestamp lastLogTimestamp) {
        Map<String, String> labels = null;
        if (event.getActor() != null) {
            labels = event.getActor().getAttributes();
        }

        this.containerId = event.getId();
        this.image = event.getFrom();
        this.log = log;
        this.stopTimestamp = new Timestamp(event.getTime() * 1000);
        this.lastLogTimestamp = lastLogTimestamp;

        if (labels != null) {
            this.containerName = labels.get("name");
            this.serverName = labels.get("ServerName");
            this.nodeId = labels.get("com.docker.swarm.node.id");
            try {
                this.exitCode = Short.parseShort(labels.get("exitCode"));
            } catch (NumberFormatException ignored) {
            }
        }

    }


}
