package minecraftplugin.minecraftplugin;

import de.derteufelqwe.commons.misc.MetaDataBase;
import lombok.Getter;

/**
 * Container specific information
 */
@Getter
public class ContainerMetaData extends MetaDataBase {

    private String taskName;
    private String serverName;
    private String containerIp;
    private int softPlayerLimit;

    public ContainerMetaData() {
        this.taskName = this.getString("TASK_NAME");
        this.serverName = this.getString("SERVER_NAME");
        this.softPlayerLimit = this.getInt("SOFT_PLAYER_LIMIT");
        this.containerIp = this.overnetIp();
    }

}
