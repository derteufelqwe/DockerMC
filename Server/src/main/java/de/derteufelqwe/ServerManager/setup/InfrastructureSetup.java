package de.derteufelqwe.ServerManager.setup;

import com.google.inject.Inject;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.setup.infrastructure.*;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.ServerManager.utils.RegistryCertPath;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;

/**
 * Class to create the default infrastructure
 */
public class InfrastructureSetup {

    private OvernetNetwork overnetNetwork = new OvernetNetwork();
    private RegistryCertificates registryCertificates;
    private RegistryContainer registryContainer;
    private NodeWatcherService nodeWatcherService = new NodeWatcherService();
    private RedisContainer redisContainer = new RedisContainer();


    @Inject
    public InfrastructureSetup(Docker docker, Config<MainConfig> mainConfig, @RegistryCertPath String certBindPath) {
        this.overnetNetwork.init(docker);
        this.registryCertificates = new RegistryCertificates(mainConfig.get());
        this.registryContainer = new RegistryContainer(certBindPath);
        this.registryContainer.init(docker, mainConfig);
        this.nodeWatcherService.init(docker, mainConfig);
        this.redisContainer.init(docker, mainConfig);
    }

    // -----  Infrastructure setup  -----

    public ServiceCreateResponse createOvernetNetwork() {
        ServiceCreateResponse response = new ServiceCreateResponse("OvernetCreation", Constants.ContainerType.OVERNET);

        DockerObjTemplate.FindResponse findResponse = overnetNetwork.find();

        // Network existing
        if (findResponse.isFound()) {
            response.setResult(ServiceStart.RUNNING);
            response.setServiceId(findResponse.getServiceID());

        // Network not found
        } else {
            DockerObjTemplate.CreateResponse createResponse = overnetNetwork.create();

            if (createResponse.isCreated()) {
                response.setResult(ServiceStart.OK);
                response.setServiceId(createResponse.getServiceID());

            } else {
                response.setResult(ServiceStart.FAILED_GENERIC);
            }
        }

        return response;
    }

    public ServiceCreateResponse createRegistryCerts() {
        ServiceCreateResponse response = new ServiceCreateResponse("RegistryCerts", Constants.ContainerType.REGISTRY_CERTS_GEN);

        if (!this.registryCertificates.find().filesExist()) {
            RegistryCertificates.RegistryCertFiles files = this.registryCertificates.create();
            response.setServiceId("RegistryCertificates");

            if (files.filesExist()) {
                response.setResult(ServiceStart.OK);

            } else {
                response.setResult(ServiceStart.FAILED_GENERIC);
            }
        } else {
            response.setResult(ServiceStart.RUNNING);
        }

        return response;
    }

    public ServiceCreateResponse createRegistryContainer() {
        ServiceCreateResponse response = new ServiceCreateResponse("Registry", Constants.ContainerType.REGISTRY);

        if (!this.registryContainer.find().isFound()) {
            DockerObjTemplate.CreateResponse createResponse = this.registryContainer.create();
            response.setServiceId(createResponse.getServiceID());

            if (createResponse.isCreated()) {
                response.setResult(ServiceStart.OK);

            } else {
                response.setResult(ServiceStart.FAILED_GENERIC);
                response.setAdditionalInfos(createResponse.getMessage());
            }

        } else {
            response.setResult(ServiceStart.RUNNING);
        }

        return response;
    }

    public ServiceCreateResponse createNodeWatcherService() {
        ServiceCreateResponse response = new ServiceCreateResponse("NodeWatcher", Constants.ContainerType.NODE_WATCHER);

        if (!this.nodeWatcherService.find().isFound()) {
            DockerObjTemplate.CreateResponse createResponse = this.nodeWatcherService.create();
            response.setServiceId(createResponse.getServiceID());

            if (createResponse.isCreated()) {
                response.setResult(ServiceStart.OK);

            } else {
                response.setResult(ServiceStart.FAILED_GENERIC);
                response.setAdditionalInfos(createResponse.getMessage());
            }

        } else {
            response.setResult(ServiceStart.RUNNING);
        }

        return response;
    }

    public ServiceCreateResponse createRedisContainer() {
        ServiceCreateResponse response = new ServiceCreateResponse("Redis", Constants.ContainerType.REDIS_DB);

        if (!this.redisContainer.find().isFound()) {
            DockerObjTemplate.CreateResponse createResponse = this.redisContainer.create();
            response.setServiceId(createResponse.getServiceID());

            if (createResponse.isCreated()) {
                response.setResult(ServiceStart.OK);

            } else {
                response.setResult(ServiceStart.FAILED_GENERIC);
                response.setAdditionalInfos(createResponse.getMessage());
            }

        } else {
            response.setResult(ServiceStart.RUNNING);
        }

        return response;
    }

    // -----  Infrastructure destruction  -----

    public ServiceStopResponse stopRegistryContainer() {
        ServiceStopResponse response = new ServiceStopResponse("Registry", Constants.ContainerType.REGISTRY);

        if (this.registryContainer.find().isFound()) {
            DockerObjTemplate.DestroyResponse destroyResponse = this.registryContainer.destroy();

            if (destroyResponse.isDestroyed()) {
                response.setResult(ServiceStop.OK);

            } else {
                response.setResult(ServiceStop.FAILED_GENERIC);
                response.setAdditionalInfos(destroyResponse.getServiceID());
            }

        } else {
            response.setResult(ServiceStop.NOT_RUNNING);
        }

        return response;
    }

    public ServiceStopResponse stopNodeWatcherService() {
        ServiceStopResponse response = new ServiceStopResponse("NodeWatcher", Constants.ContainerType.NODE_WATCHER);

        if (this.nodeWatcherService.find().isFound()) {
            DockerObjTemplate.DestroyResponse destroyResponse = this.nodeWatcherService.destroy();

            if (destroyResponse.isDestroyed()) {
                response.setResult(ServiceStop.OK);

            } else {
                response.setResult(ServiceStop.FAILED_GENERIC);
                response.setAdditionalInfos(destroyResponse.getServiceID());
            }

        } else {
            response.setResult(ServiceStop.NOT_RUNNING);
        }

        return response;
    }

    public ServiceStopResponse stopRedisContainer() {
        ServiceStopResponse response = new ServiceStopResponse("Redis", Constants.ContainerType.REDIS_DB);

        if (this.redisContainer.find().isFound()) {
            DockerObjTemplate.DestroyResponse destroyResponse = this.redisContainer.destroy();

            if (destroyResponse.isDestroyed()) {
                response.setResult(ServiceStop.OK);

            } else {
                response.setResult(ServiceStop.FAILED_GENERIC);
                response.setAdditionalInfos(destroyResponse.getServiceID());
            }

        } else {
            response.setResult(ServiceStop.NOT_RUNNING);
        }

        return response;
    }

}
