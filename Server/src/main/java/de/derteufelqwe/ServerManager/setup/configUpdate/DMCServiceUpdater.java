package de.derteufelqwe.ServerManager.setup.configUpdate;

import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.api.model.ServiceSpec;
import com.github.dockerjava.api.model.UpdateConfig;
import com.sun.istack.internal.NotNull;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.SystemConfig;
import de.derteufelqwe.ServerManager.setup.ServiceCreateResponse;
import de.derteufelqwe.ServerManager.setup.ServiceStart;
import de.derteufelqwe.ServerManager.setup.ServiceUpdate;
import de.derteufelqwe.ServerManager.setup.ServiceUpdateResponse;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.ServerManager.setup.templates.ServiceTemplate;
import de.derteufelqwe.commons.Constants;

import javax.annotation.Nullable;

/**
 * Base class for every MC server config service update
 *
 * @param <CFG> Type of the config like {@link de.derteufelqwe.ServerManager.setup.servers.BungeePool}
 *              or {@link de.derteufelqwe.ServerManager.setup.servers.ServerPool}
 */
abstract class DMCServiceUpdater<CFG extends ServiceTemplate> {

    protected InfrastructureConfig infrastructureConfig = ServerManager.CONFIG.get(InfrastructureConfig.class);
    protected SystemConfig systemConfig = ServerManager.CONFIG.get(SystemConfig.class);

    protected Docker docker;

    protected ServiceUpdateResponse response;
    protected CFG configNew;
    protected CFG configOld;


    public DMCServiceUpdater(Docker docker) {
        this.docker = docker;
    }


    /**
     * Returns the new config instance.
     */
    protected abstract CFG getNewConfig();

    /**
     * Returns the old config instance
     */
    protected abstract CFG getOldConfig();

    /**
     * Updates the old config instance.
     *
     * @param configObj Can be null or a config instance
     */
    protected abstract void setOldConfig(@Nullable CFG configObj);

    /**
     * Returns the ContainerType used in this service
     */
    protected abstract Constants.ContainerType getContainerType();

    /**
     * This method only needs to call and return the corrent instance using a class based on {@link DMCServiceCreator}
     */
    protected abstract ServiceCreateResponse createNewService();

    /**
     * Must return the wanted update config for the docker service.
     */
    protected abstract UpdateConfig getUpdateConfig();

    /**
     * Increments the force update counter to enable force updates.
     */
    protected int getForceUpdateCounter(ServiceSpec spec) {
        int start = spec.getTaskTemplate().getForceUpdate();

        if (start < Integer.MAX_VALUE) {
            return start + 1;
        }

        return 0;
    }

    /**
     * Updates the docker service.
     *
     * @param response  Response to save information to
     * @param newSpec   The ServiceSpec to update to
     * @param serviceId The ServiceID of the service getting updated
     */
    protected void updateDockerService(ServiceUpdateResponse response, ServiceSpec newSpec, String serviceId) {
        Service service = docker.getDocker().inspectServiceCmd(serviceId).exec();
        newSpec.getTaskTemplate().withForceUpdate(this.getForceUpdateCounter(service.getSpec()));
        newSpec.withUpdateConfig(this.getUpdateConfig());

        docker.getDocker().updateServiceCmd(serviceId, newSpec)
                .withVersion(service.getVersion().getIndex())
                .exec();

        response.setResult(ServiceUpdate.UPDATED);
        response.setServiceId(service.getId());

    }

    /**
     * Wrapper for updateDockerService
     */
    protected void createService(ServiceUpdateResponse response) {
        ServiceCreateResponse serviceCreateResponse = this.createNewService();
        response.setServiceId(serviceCreateResponse.getServiceId());

        if (serviceCreateResponse.getResult() == ServiceStart.OK) {
            response.setResult(ServiceUpdate.CREATED);
            this.setOldConfig((CFG) this.configNew.clone());

        } else if (serviceCreateResponse.getResult() == ServiceStart.RUNNING) {
            response.setResult(ServiceUpdate.NOT_REQUIRED);
            this.setOldConfig((CFG) this.configNew.clone());

        } else {
            response.setResult(ServiceUpdate.FAILED_GENERIC);
        }
    }


    private void onBothServicesNull() {
        response.setResult(ServiceUpdate.NOT_CONFIGURED);
    }

    private void onServiceToCreate() {
        configNew.init(docker);
        this.createService(response);
    }

    private void onServiceToDestroy() {
        configOld.init(docker);

        if (configOld.find().isFound()) {
            DockerObjTemplate.DestroyResponse destroyResponse = configOld.destroy();
            response.setServiceId(destroyResponse.getServiceID());

            if (destroyResponse.isDestroyed()) {
                response.setResult(ServiceUpdate.DESTROYED);
                this.setOldConfig(null);

            } else {
                response.setResult(ServiceUpdate.FAILED_GENERIC);
            }
        }
    }

    private void onServiceToUpdate() {
        configNew.init(docker);

        DockerObjTemplate.FindResponse findResponse = configNew.find();

        // Check if the service exists
        if (!findResponse.isFound()) {
            this.createService(response);

        // Update the service
        } else {
            this.updateDockerService(response, configNew.getServiceSpec(), findResponse.getServiceID());
            this.setOldConfig((CFG) configNew.clone());

        }
    }

    public void onServicesEqual(boolean force) {
        configNew.init(docker);

        DockerObjTemplate.FindResponse findResponse = configNew.find();
        response.setServiceId(findResponse.getServiceID());

        if (findResponse.isFound()) {
            response.setResult(ServiceUpdate.NOT_REQUIRED);

            // Force update even if nothing happened to update the version of the image
            if (force) {
                this.updateDockerService(response, configNew.getServiceSpec(), findResponse.getServiceID());
            }

        } else {
            response.setResult(ServiceUpdate.CREATED);
            this.createService(response);
        }
    }

    /**
     * Actual update method, that analyzes the config difference
     *
     * @param force If true also updates the config if no parameters changed
     */
    public ServiceUpdateResponse update(boolean force) {
        this.response = new ServiceUpdateResponse(this.getContainerType(), ServiceUpdate.NOT_CONFIGURED);

        this.configNew = this.getNewConfig();
        this.configOld = this.getOldConfig();

        // Nothing configured -> No action
        if (configNew == null && configOld == null) {
            this.onBothServicesNull();


            // Only old config is null -> Create Service
        } else if (configNew != null && configOld == null) {
            this.onServiceToCreate();


            // Only the new config is null -> Destroy Service
        } else if (configNew == null && configOld != null) {
            this.onServiceToDestroy();


            // Both configs not null and unequal -> Update Service
        } else if (!configNew.equals(configOld)) {
            this.onServiceToUpdate();


            // Both configs are equal
        } else if (configNew.equals(configOld)) {
            this.onServicesEqual(force);


            // Should not occur
        } else {
            throw new RuntimeException("Invalid config update state.");
        }

        return response;
    }

}
