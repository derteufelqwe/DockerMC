package de.derteufelqwe.ServerManager.setup.configUpdate;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.config.SystemConfig;
import de.derteufelqwe.ServerManager.setup.ServiceCreateResponse;
import de.derteufelqwe.ServerManager.setup.ServiceStart;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.ServerManager.setup.templates.ServiceTemplate;
import de.derteufelqwe.commons.Constants;

/**
 * Base class for every MC server config service creation
 *
 * @param <CFG> Type of the config like {@link de.derteufelqwe.ServerManager.setup.servers.BungeePool}
 *              or {@link de.derteufelqwe.ServerManager.setup.servers.ServerPool}
 */
abstract class DMCServiceCreator<CFG extends ServiceTemplate> {

    protected ServersConfig serversConfig = ServerManager.SERVERS_CONFIG.get();
    protected SystemConfig systemConfig = ServerManager.SYSTEM_CONFIG.get();

    protected Docker docker;

    private CFG dmcPool;
    private ServiceCreateResponse response;


    public DMCServiceCreator(Docker docker) {
        this.docker = docker;
    }


    /**
     * Returns the current config instance
     */
    protected abstract CFG getConfigObject();

    /**
     * Returns the container type of the pool
     */
    protected abstract Constants.ContainerType getContainerType();

    /**
     * Updates the system config with the new config
     */
    protected abstract void updateSystemConfig(CFG newData);


    private void onServiceFound(DockerObjTemplate.FindResponse findResponse) {
        response.setResult(ServiceStart.RUNNING);
        response.setServiceId(findResponse.getServiceID());
    }

    private void onServiceNotFound(DockerObjTemplate.FindResponse findResponse) {
        DockerObjTemplate.CreateResponse createResponse = dmcPool.create();
        response.setServiceId(createResponse.getServiceID());

        if (createResponse.isCreated()) {
            this.onServiceCreated(createResponse);

        } else {
            this.onServiceNotCreated(createResponse);
        }
    }

    protected void onServiceCreated(DockerObjTemplate.CreateResponse createResponse) {
        response.setResult(ServiceStart.OK);
        this.updateSystemConfig((CFG) this.dmcPool.clone());
    }

    protected void onServiceNotCreated(DockerObjTemplate.CreateResponse createResponse) {
        response.setResult(ServiceStart.FAILED_GENERIC);
        response.setAdditionalInfos(createResponse.getMessage());
    }


    /**
     * The actual creation method, which analyzes the running services and processes it.
     */
    public ServiceCreateResponse create() {
        this.dmcPool = this.getConfigObject();
        this.response = new ServiceCreateResponse(this.getContainerType());

        if (dmcPool == null) {
            response.setResult(ServiceStart.NOT_CONFIGURED);
            return response;
        }

        dmcPool.init(docker);
        response.setServiceName(dmcPool.getName());

        DockerObjTemplate.FindResponse findResponse = dmcPool.find();

        if (findResponse.isFound()) {
            this.onServiceFound(findResponse);

        } else {
            this.onServiceNotFound(findResponse);
        }

        return response;
    }

}
