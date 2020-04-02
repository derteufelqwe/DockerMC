package de.derteufelqwe.ServerManager.setup.servers.responses;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;

public abstract class Response {

    protected FailureCause failureCause = FailureCause.NOT_FAILED;
    protected String objectID;
    protected Docker docker;


    public Response(String objectID) {
        this.objectID = objectID;
        this.docker = ServerManager.getDocker();
    }


    public abstract boolean successful();


    public abstract String getLogs();


    public FailureCause getCause() {
        return this.failureCause;
    }

}
