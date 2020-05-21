package minecraftplugin.minecraftplugin;

import de.derteufelqwe.commons.MetaDataBase;
import lombok.Getter;

@Getter
public class MetaData extends MetaDataBase {

    private String taskName;
    private String serverName;
    private String containerIp;
    private int softPlayerLimit;

    public MetaData() {
        this.taskName = this.getString("TASK_NAME");
        this.serverName = this.getString("SERVER_NAME");
        if (System.getProperty("os.name").startsWith("Windows")) {
            this.containerIp = "192.168.178.2";
        } else {
            this.containerIp = this.getIP("eth0");
        }
        this.softPlayerLimit = this.getInt("SOFT_PLAYER_LIMIT");
    }

}
