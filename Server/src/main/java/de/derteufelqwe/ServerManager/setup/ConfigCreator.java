package de.derteufelqwe.ServerManager.setup;

import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.templates.ServiceTemplate;
import de.derteufelqwe.commons.Constants;
import org.jetbrains.annotations.Nullable;

public abstract class ConfigCreator<CFG extends ServiceTemplate> {

    private final MainConfig mainConfig = ServerManager.getMainConfig().get();
    @Nullable protected CFG poolConfig;
    @Nullable private CFG oldPoolConfig;
    private final Docker docker;
    private final Constants.ContainerType containerType;
    private ServiceCreateResponse response;
    private final int parallelUpdateCount = getParallelUpdateCount();


    public ConfigCreator(@Nullable CFG poolConfig, CFG oldPoolConfig, Docker docker, Constants.ContainerType containerType) {
        this.poolConfig = poolConfig;
        this.oldPoolConfig = oldPoolConfig;
        this.docker = docker;
        this.containerType = containerType;
    }


    protected abstract void updateOldConfigFile(CFG newConfig);

    protected abstract int getParallelUpdateCount();


    public ServiceCreateResponse createOrUpdate(boolean force) {
        this.response = new ServiceCreateResponse(this.containerType);

        // Nothing configured -> No action
        if (poolConfig == null && oldPoolConfig == null) {
            this.onBothServicesNull();


            // Only old config is null -> Create Service
        } else if (poolConfig != null && oldPoolConfig == null) {
            this.onServiceToCreate();


            // Only the new config is null -> Destroy Service
        } else if (poolConfig == null && oldPoolConfig != null) {
            this.onServiceToDestroy();


            // Both configs not null and unequal -> Update Service
        } else if (!poolConfig.equals(oldPoolConfig)) {
            this.onServiceToUpdate();


            // Both configs are equal
        } else if (poolConfig.equals(oldPoolConfig)) {
            this.onServicesEqual(force);


            // Should not occur
        } else {
            throw new RuntimeException("Invalid config update state.");
        }

        return response;
    }

    private ServiceCreateResponse createService() {
        if (poolConfig == null) {
            response.setResult(ServiceStart.NOT_CONFIGURED);
            return response;
        }

        poolConfig.init(docker);
        response.setServiceName(poolConfig.getName());

        DockerObjTemplate.FindResponse findResponse = poolConfig.find();
        if (findResponse.isFound()) {
            this.onServiceFound(findResponse);

        } else {
            this.onServiceNotFound(findResponse);
        }

        return response;
    }

    // -----  Create methods  -----

    private void onServiceFound(DockerObjTemplate.FindResponse findResponse) {
        response.setResult(ServiceStart.RUNNING);
        response.setServiceId(findResponse.getServiceID());
    }

    private void onServiceNotFound(DockerObjTemplate.FindResponse findResponse) {
        DockerObjTemplate.CreateResponse createResponse = poolConfig.create();
        response.setServiceId(createResponse.getServiceID());

        if (createResponse.isCreated()) {
            this.onServiceCreated(createResponse);

        } else {
            this.onServiceNotCreated(createResponse);
        }
    }

    protected void onServiceCreated(DockerObjTemplate.CreateResponse createResponse) {
        response.setResult(ServiceStart.CREATED);
        this.updateOldConfigFile((CFG) poolConfig.clone());
    }

    protected void onServiceNotCreated(DockerObjTemplate.CreateResponse createResponse) {
        response.setResult(ServiceStart.FAILED_GENERIC);
        response.setAdditionalInfos(createResponse.getMessage());
    }

    // -----  Update methods  -----

    /**
     * Updates the docker service.
     *
     * @param newSpec   The ServiceSpec to update to
     * @param serviceId The ServiceID of the service getting updated
     */
    protected void updateDockerService(ServiceSpec newSpec, String serviceId) {
        Service service = docker.getDocker().inspectServiceCmd(serviceId).exec();

        newSpec.getTaskTemplate().withForceUpdate(this.getForceUpdateCounter(service.getSpec()));
        newSpec.withUpdateConfig(this.getUpdateConfig());

        docker.getDocker().updateServiceCmd(serviceId, newSpec)
                .withVersion(service.getVersion().getIndex())
                .exec();

        response.setResult(ServiceStart.UPDATED);
        response.setServiceId(service.getId());
    }

    private void onBothServicesNull() {
        response.setResult(ServiceStart.NOT_CONFIGURED);
    }

    private void onServiceToCreate() {
        poolConfig.init(docker);
        this.createService();
    }

    private void onServiceToDestroy() {
        poolConfig.init(docker);

        if (oldPoolConfig.find().isFound()) {
            DockerObjTemplate.DestroyResponse destroyResponse = oldPoolConfig.destroy();
            response.setServiceId(destroyResponse.getServiceID());

            if (destroyResponse.isDestroyed()) {
                response.setResult(ServiceStart.DESTROYED);
                this.updateOldConfigFile(null);

            } else {
                response.setResult(ServiceStart.FAILED_GENERIC);
            }
        }
    }

    private void onServiceToUpdate() {
        poolConfig.init(docker);

        DockerObjTemplate.FindResponse findResponse = poolConfig.find();

        // Check if the service exists
        if (!findResponse.isFound()) {
            this.createService();

            // Update the service
        } else {
            this.updateDockerService(poolConfig.getServiceSpec(), findResponse.getServiceID());
            this.updateOldConfigFile((CFG) poolConfig.clone());
        }
    }

    private void onServicesEqual(boolean force) {
        poolConfig.init(docker);

        DockerObjTemplate.FindResponse findResponse = poolConfig.find();
        response.setServiceId(findResponse.getServiceID());

        if (findResponse.isFound()) {
            response.setResult(ServiceStart.RUNNING);

            // Force update even if nothing happened to update the version of the image
            if (force) {
                this.updateDockerService(poolConfig.getServiceSpec(), findResponse.getServiceID());
            }

        } else {
            response.setResult(ServiceStart.CREATED);
            this.createService();
        }
    }

    // -----  Service update utility methods  -----

    /**
     * Increments the force update counter to enable force updates. This is required for the docker api
     */
    protected int getForceUpdateCounter(ServiceSpec spec) {
        int start = spec.getTaskTemplate().getForceUpdate();

        if (start < Integer.MAX_VALUE) {
            return start + 1;
        }

        return 0;
    }

    /**
     * Returns the update order. If a node limit is set it's STOP_FIRST, otherwise START_FIRST.
     * The config can force a
     */
    private UpdateOrder getUpdateOrder() {
        if (mainConfig.isForceStopFirst())
            return UpdateOrder.STOP_FIRST;

        ServiceConstraints constraints = poolConfig.getConstraints();
        if (constraints == null)
            return UpdateOrder.START_FIRST;

        if (constraints.getNodeLimit() <= 0)
            return UpdateOrder.START_FIRST;

        return UpdateOrder.STOP_FIRST;
    }

    private UpdateConfig getUpdateConfig() {
        return new UpdateConfig()
                .withParallelism(this.parallelUpdateCount)
                .withOrder(this.getUpdateOrder())
                .withFailureAction(UpdateFailureAction.CONTINUE)
                ;
    }

}
