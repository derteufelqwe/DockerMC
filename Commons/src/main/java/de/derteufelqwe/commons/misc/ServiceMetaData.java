package de.derteufelqwe.commons.misc;

import lombok.Getter;

/**
 * Reads the default properties, which every service container has.
 */
@Getter
public class ServiceMetaData extends MetaDataBase {

    private String serviceID;
    private String nodeID;
    private String taskName;

    public ServiceMetaData() {
        super();
        this.serviceID = this.getString("SERVICE_ID");
        this.nodeID = this.getString("NODE_ID");
        this.taskName = this.getString("TASK_NAME");
    }
}
