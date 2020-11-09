package de.derteufelqwe.bungeeplugin.utils;

import de.derteufelqwe.commons.MetaDataBase;
import lombok.Getter;

/**
 * Contains all the metadata for the container
 */
@Getter
public class MetaData extends MetaDataBase {

    private String containerIP;
    /**
     * Name of the task of the docker service.
     * Example:     BungeePool.2.xmg5hinf5qjh4c20fnma0ugqg
     * Explanation: ServiceName.InstanceNumber.TaskId
     */
    private String taskName;

    public MetaData() {
        this.containerIP = this.overnetIp();
        this.taskName = this.getString("TASK_NAME");
    }


}
