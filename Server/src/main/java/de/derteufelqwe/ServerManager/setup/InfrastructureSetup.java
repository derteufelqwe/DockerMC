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
    private LogCollectorService logCollectorService = new LogCollectorService();
    private RedisContainer redisContainer = new RedisContainer();


    public InfrastructureSetup(Docker docker) {
        this.docker = docker;
        this.overnetNetwork.init(docker);
        this.registryCertificates = new RegistryCertificates(docker);
        this.registryContainer.init(docker);
        this.postgresDBContainer.init(docker);
        this.logCollectorService.init(docker);
        this.redisContainer.init(docker);
    }


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

    public ServiceCreateResponse createPostgresContainer() {
        ServiceCreateResponse response = new ServiceCreateResponse("PostgresDatabase", Constants.ContainerType.POSTGRES_DB);

        if (!this.postgresDBContainer.find().isFound()) {
            DockerObjTemplate.CreateResponse createResponse = this.postgresDBContainer.create();
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

    public ServiceCreateResponse createLogcollectorService() {
        ServiceCreateResponse response = new ServiceCreateResponse("LogCollector", Constants.ContainerType.LOGCOLLECTOR);

        if (!this.logCollectorService.find().isFound()) {
            DockerObjTemplate.CreateResponse createResponse = this.logCollectorService.create();
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

}
