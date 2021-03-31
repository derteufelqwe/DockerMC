package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.infrastructure.*;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.commons.Constants;

/**
 * Class to create the default infrastructure
 */
public class InfrastructureSetup {

    private Docker docker;

    private OvernetNetwork overnetNetwork = new OvernetNetwork();
    private RegistryCertificates registryCertificates;
    private RegistryContainer registryContainer = new RegistryContainer();
    private PostgresDBContainer postgresDBContainer = new PostgresDBContainer();
    private NodeWatcherService nodeWatcherService = new NodeWatcherService();
    private RedisContainer redisContainer = new RedisContainer();


    public InfrastructureSetup(Docker docker) {
        this.docker = docker;
        this.overnetNetwork.init(docker);
        this.registryCertificates = new RegistryCertificates(docker);
        this.registryContainer.init(docker);
        this.postgresDBContainer.init(docker);
        this.nodeWatcherService.init(docker);
        this.redisContainer.init(docker);
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

        if (!this.registryCertificates.find().isFound()) {
            DockerObjTemplate.CreateResponse createResponse = this.registryCertificates.create();
            response.setServiceId(createResponse.getServiceID());

            if (this.registryCertificates.find().isFound()) {
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

    public ServiceStopResponse stopPostgresContainer() {
        ServiceStopResponse response = new ServiceStopResponse("PostgresDatabase", Constants.ContainerType.POSTGRES_DB);

        if (this.postgresDBContainer.find().isFound()) {
            DockerObjTemplate.DestroyResponse destroyResponse = this.postgresDBContainer.destroy();

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
