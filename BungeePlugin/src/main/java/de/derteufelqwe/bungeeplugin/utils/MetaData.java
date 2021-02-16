package de.derteufelqwe.bungeeplugin.utils;

import de.derteufelqwe.commons.misc.MetaDataBase;
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
    private String serviceName;
    private int slot = -1;
    private String taskId;

    public MetaData() {
        this.containerIP = this.overnetIp();
        this.taskName = this.getString("TASK_NAME");

        if (this.taskName != null) {
            String[] split = this.taskName.split("\\.");
            this.serviceName = split[0];
            this.slot = Integer.parseInt(split[1]);
            this.taskId = split[2];

            assert this.serviceName.equals(this.getString("SERVER_NAME"));
        }
    }


}
