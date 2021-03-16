package de.derteufelqwe.minecraftplugin;

import de.derteufelqwe.commons.misc.ServiceMetaData;
import lombok.Getter;

/**
 * Minecraft service container specific information
 */
@Getter
public class ContainerMetaData extends ServiceMetaData {

    private String serverName;
    private String containerIP;
    private int softPlayerLimit;

    public ContainerMetaData() {
        this.serverName = this.getString("SERVER_NAME");
        this.containerIP = this.overnetIp();
        this.softPlayerLimit = this.getInt("SOFT_PLAYER_LIMIT");
    }

}
