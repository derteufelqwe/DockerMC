package de.derteufelqwe.bungeeplugin;

import de.derteufelqwe.commons.MetaDataBase;
import lombok.Getter;

/**
 * Contains all the metadata for the container
 */
@Getter
public class MetaData extends MetaDataBase {

    private String containerIP;
    private String taskName;

    public MetaData() {
        this.containerIP = this.overnetIp();
        this.taskName = this.getString("TASK_NAME");
    }


}
