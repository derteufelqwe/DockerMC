package de.derteufelqwe.ServerManager.setup;

import com.github.dockerjava.api.model.*;
import com.orbitz.consul.KeyValueClient;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.SystemConfig;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.commons.Constants;

/**
 * Responsible for updating the Services
 */
@Deprecated
public class ServerConfigUpdater {

    private InfrastructureConfig infrastructureConfig = ServerManager.CONFIG.get(InfrastructureConfig.class);
    private SystemConfig systemConfig = ServerManager.CONFIG.get(SystemConfig.class);

    private Docker docker;
    private KeyValueClient kvClient;
    private MCServerConfigSetup mcServerConfigSetup;


    public ServerConfigUpdater(Docker docker, KeyValueClient kvClient) {
        this.docker = docker;
        this.kvClient = kvClient;
        this.mcServerConfigSetup = new MCServerConfigSetup(docker, kvClient);
    }


    /**
     * Creates a new docker service for the BungeeCord-Pool
     * @param response
     */
    private void createBungeePool(ServiceUpdateResponse response) {
        ServiceCreateResponse serviceCreateResponse = this.mcServerConfigSetup.createBungeePool();
        response.setServiceId(serviceCreateResponse.getServiceId());

        if (serviceCreateResponse.getResult() == ServiceStart.OK) {
            response.setResult(ServiceUpdate.CREATED);

        } else if (serviceCreateResponse.getResult() == ServiceStart.RUNNING) {
            response.setResult(ServiceUpdate.NOT_REQUIRED);

        } else {
            response.setResult(ServiceUpdate.FAILED_GENERIC);
        }
    }

    /**
     * Performs a docker update for the BungeeCord-Pool
     * @param response
     * @param newSpec
     * @param serviceId
     */
    private void updateOnlyBungeePool(ServiceUpdateResponse response, ServiceSpec newSpec, String serviceId) {
        Service service = docker.getDocker().inspectServiceCmd(serviceId).exec();
        newSpec.getTaskTemplate().withForceUpdate(1000);
        newSpec.withUpdateConfig(new UpdateConfig()
                .withParallelism(1)
                .withOrder(UpdateOrder.STOP_FIRST)
                .withFailureAction(UpdateFailureAction.CONTINUE)
        );

        docker.getDocker().updateServiceCmd(serviceId, newSpec)
                .withVersion(service.getVersion().getIndex())
                .exec();

        response.setResult(ServiceUpdate.UPDATED);
        response.setServiceId(service.getId());
    }

    /**
     * Updates the BungeePool service.
     * @param force If true also performs a docker update if the config didn't change. This is useful
     *              to update the version of the image the service uses.
     */
    public ServiceUpdateResponse updateBungeePool(boolean force) {
        ServiceUpdateResponse response = new ServiceUpdateResponse(Constants.ContainerType.BUNGEE_POOL, ServiceUpdate.NOT_CONFIGURED);

        BungeePool bungeePoolNew = this.infrastructureConfig.getBungeePool();
        BungeePool bungeePoolOld = this.systemConfig.getBungeePool();

        // Nothing configured -> No action
        if (bungeePoolNew == null && bungeePoolOld == null) {
            response.setResult(ServiceUpdate.NOT_CONFIGURED);

        // Only old config is null -> Create Service
        } else if (bungeePoolNew != null && bungeePoolOld == null) {
            bungeePoolNew.init(docker);
            this.createBungeePool(response);


        // Only the new config is null -> Destroy Service
        } else if (bungeePoolNew == null && bungeePoolOld != null) {
            bungeePoolOld.init(docker);

            if (bungeePoolOld.find().isFound()) {
                DockerObjTemplate.DestroyResponse destroyResponse = bungeePoolOld.destroy();
                response.setServiceId(destroyResponse.getServiceID());

                if (destroyResponse.isDestroyed()) {
                    response.setResult(ServiceUpdate.DESTROYED);
                    this.systemConfig.setBungeePool(null);

                } else {
                    response.setResult(ServiceUpdate.FAILED_GENERIC);
                }
            }

        // Both configs not null and unequal -> Update Service
        } else if (!bungeePoolNew.equals(bungeePoolOld)) {
            bungeePoolNew.init(docker);

            DockerObjTemplate.FindResponse findResponse = bungeePoolNew.find();

            // Check if the service exists
            if (!findResponse.isFound()) {
                this.createBungeePool(response);

            // Update the service
            } else {
                this.updateOnlyBungeePool(response, bungeePoolNew.getServiceSpec(), findResponse.getServiceID());
                this.systemConfig.setBungeePool((BungeePool) bungeePoolNew.clone());

            }

        // Both configs are equal
        } else if (bungeePoolNew.equals(bungeePoolOld)) {
            bungeePoolNew.init(docker);

            DockerObjTemplate.FindResponse findResponse = bungeePoolNew.find();
            response.setServiceId(findResponse.getServiceID());

            if (findResponse.isFound()) {
                response.setResult(ServiceUpdate.NOT_REQUIRED);
                // Force update even if nothing happened to update the version of the image
                if (force) {
                    this.updateOnlyBungeePool(response, bungeePoolNew.getServiceSpec(), findResponse.getServiceID());
                }

            } else {
                response.setResult(ServiceUpdate.CREATED);
                this.createBungeePool(response);
            }

        // Should not occur
        } else {
            throw new RuntimeException("Invalid config update state.");
        }

        return response;
    }


    /**
     * Creates a new docker service for the BungeeCord-Pool
     * @param response
     */
    private void createServerPool(ServiceUpdateResponse response) {
        ServiceCreateResponse serviceCreateResponse = this.mcServerConfigSetup.createLobbyPool();
        response.setServiceId(serviceCreateResponse.getServiceId());

        if (serviceCreateResponse.getResult() == ServiceStart.OK) {
            response.setResult(ServiceUpdate.CREATED);

        } else if (serviceCreateResponse.getResult() == ServiceStart.RUNNING) {
            response.setResult(ServiceUpdate.NOT_REQUIRED);

        } else {
            response.setResult(ServiceUpdate.FAILED_GENERIC);
        }
    }

    /**
     * Performs a docker update for the BungeeCord-Pool
     * @param response
     * @param newSpec
     * @param serviceId
     */
    private void updateOnlyServerPool(ServiceUpdateResponse response, ServiceSpec newSpec, String serviceId) {
        Service service = docker.getDocker().inspectServiceCmd(serviceId).exec();
        newSpec.getTaskTemplate().withForceUpdate(1000);
        newSpec.withUpdateConfig(new UpdateConfig()
                .withParallelism(1)
                .withOrder(UpdateOrder.START_FIRST)
                .withFailureAction(UpdateFailureAction.CONTINUE)
        );

        docker.getDocker().updateServiceCmd(serviceId, newSpec)
                .withVersion(service.getVersion().getIndex())
                .exec();

        response.setResult(ServiceUpdate.UPDATED);
        response.setServiceId(service.getId());
    }

    private ServiceUpdateResponse updateServerPool(boolean force) {
        ServiceUpdateResponse response = new ServiceUpdateResponse(Constants.ContainerType.MINECRAFT_POOL, ServiceUpdate.NOT_CONFIGURED);

        ServerPool serverPoolNew = this.infrastructureConfig.getLobbyPool();
        ServerPool serverPoolOld = this.systemConfig.getLobbyPool();

        // Nothing configured -> No action
        if (serverPoolNew == null && serverPoolOld == null) {
            response.setResult(ServiceUpdate.NOT_CONFIGURED);

        // Only old config is null -> Create Service
        } else if (serverPoolNew != null && serverPoolOld == null) {
            serverPoolNew.init(docker);
            this.createServerPool(response);


        // Only the new config is null -> Destroy Service
        } else if (serverPoolNew == null && serverPoolOld != null) {
            serverPoolOld.init(docker);

            if (serverPoolOld.find().isFound()) {
                DockerObjTemplate.DestroyResponse destroyResponse = serverPoolOld.destroy();
                response.setServiceId(destroyResponse.getServiceID());

                if (destroyResponse.isDestroyed()) {
                    response.setResult(ServiceUpdate.DESTROYED);
                    this.systemConfig.setLobbyPool(null);

                } else {
                    response.setResult(ServiceUpdate.FAILED_GENERIC);
                }
            }

        // Both configs not null and unequal -> Update Service
        } else if (!serverPoolNew.equals(serverPoolOld)) {
            serverPoolNew.init(docker);

            DockerObjTemplate.FindResponse findResponse = serverPoolNew.find();

            // Check if the service exists
            if (!findResponse.isFound()) {
                this.createServerPool(response);

                // Update the service
            } else {
                this.updateOnlyServerPool(response, serverPoolNew.getServiceSpec(), findResponse.getServiceID());
                this.systemConfig.setLobbyPool((ServerPool) serverPoolNew.clone());

            }

        // Both configs are equal
        } else if (serverPoolNew.equals(serverPoolOld)) {
            serverPoolNew.init(docker);

            DockerObjTemplate.FindResponse findResponse = serverPoolNew.find();
            response.setServiceId(findResponse.getServiceID());

            if (findResponse.isFound()) {
                response.setResult(ServiceUpdate.NOT_REQUIRED);
                // Force update even if nothing happened to update the version of the image
                if (force) {
                    this.updateOnlyServerPool(response, serverPoolNew.getServiceSpec(), findResponse.getServiceID());
                }

            } else {
                response.setResult(ServiceUpdate.CREATED);
                this.createServerPool(response);
            }

        // Should not occur
        } else {
            throw new RuntimeException("Invalid config update state.");
        }

        return response;
    }


    public ServiceUpdateResponse updateLobbyPool(boolean force) {
        return this.updateServerPool(force);
    }

}
