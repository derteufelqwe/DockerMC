package de.derteufelqwe.commons.docker;

import de.derteufelqwe.commons.exceptions.InvalidServerName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Utility class to represent a Server name like Lobby-1
 */
@Getter
@Setter
@EqualsAndHashCode
public class ServerName {

    private String serverName;
    private String instanceName;


    public ServerName(String compositServerName) {
        String[] splits = compositServerName.split("-");
        if (splits.length < 2) {
            throw new InvalidServerName("Invalid server name '%s'. The name must contain a '-'.", compositServerName);
        } else if (splits.length > 2) {
            throw new InvalidServerName("Invalid server name '%s'. The name can only contain one '-'.", compositServerName);
        }

        this.serverName = splits[0];
        this.instanceName = splits[1];
    }

    public ServerName(String serverName, String instanceName) {
        this.serverName = serverName;
        this.instanceName = instanceName;
    }


    public String fullName() {
        return String.format("%s-%s", this.serverName, this.instanceName);
    }


    @Override
    public String toString() {
        return String.format("MCServer<%s-%s>", this.serverName, this.instanceName);
    }

}
