package de.derteufelqwe.ServerManager.setup;

import com.github.dockerjava.api.model.*;
import com.orbitz.consul.KeyValueClient;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.SystemConfig;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.commons.Constants;

/**
 * Responsible for updating the Services
 */
public class ServerConfigUpdater {

    /**
     * Idee:
     * 2. Server Config, die die "alte" Version beinhaltet
     *
     */

    private InfrastructureConfig infrastructureConfig = ServerManager.CONFIG.get(InfrastructureConfig.class);
    private SystemConfig systemConfig = ServerManager.CONFIG.get(SystemConfig.class);

    private Docker docker;
    private KeyValueClient kvClient;
    private MCServerConfigSetup mcServerConfigSetup;


    public ServerConfigUpdater(Docker docker, KeyValueClient kvClient) {
        this.docker = docker;
        this.kvClient = kvClient;
        this.mcServerConfigSetup = new MCServerConfigSetup(docker, kvClient, true);
    }


    private void createBungeePool(ServiceUpdateResponse response) {
        ServiceCreateResponse serviceCreateResponse = this.mcServerConfigSetup.createBungeePool();
        response.setServiceId(serviceCreateResponse.getServiceId());

        if (serviceCreateResponse.getResult() == ServiceStart.OK) {
            response.setResult(ServiceUpdate.CREATED);

        } else if (serviceCreateResponse.getResult() == ServiceStart.RUNNING) {
            response.setResult(ServiceUpdate.EXISTING);

        } else {
            response.setResult(ServiceUpdate.FAILED_GENERIC);
        }
    }

    /**
     * Updates the BungeePool
     * @return True if an update happened.
     */
    public ServiceUpdateResponse updateBungeePool() {
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
                Service service = docker.getDocker().inspectServiceCmd(findResponse.getServiceID()).exec();
                ServiceSpec newSpec = bungeePoolNew.getServiceSpec();
                newSpec.getTaskTemplate().withForceUpdate(1000);
                newSpec.withUpdateConfig(new UpdateConfig()
                        .withParallelism(1)
                        .withOrder(UpdateOrder.STOP_FIRST)
                        .withFailureAction(UpdateFailureAction.CONTINUE)
                );

                docker.getDocker().updateServiceCmd(findResponse.getServiceID(), newSpec)
                        .withVersion(service.getVersion().getIndex())
                        .exec();

                response.setResult(ServiceUpdate.UPDATED);
                response.setServiceId(service.getId());
                this.systemConfig.setBungeePool((BungeePool) bungeePoolNew.clone());

            }

        // Both configs are equal
        } else if (bungeePoolNew.equals(bungeePoolOld)) {
            bungeePoolNew.init(docker);

            DockerObjTemplate.FindResponse findResponse = bungeePoolNew.find();

            response.setResult(ServiceUpdate.NOT_REQUIRED);
            response.setServiceId(findResponse.getServiceID());

        // Should not occur
        } else {
            throw new RuntimeException("Invalid config update state.");
        }

        return response;
    }


}
