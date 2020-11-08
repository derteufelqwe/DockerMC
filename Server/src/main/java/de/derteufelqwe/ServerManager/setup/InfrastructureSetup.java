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
    private ConsulContainer consulContainer = new ConsulContainer();



    public InfrastructureSetup(Docker docker) {
        this.docker = docker;
        this.overnetNetwork.init(docker);
        this.registryCertificates = new RegistryCertificates(docker);
        this.registryContainer.init(docker);
        this.consulContainer.init(docker);
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

    public ServiceCreateResponse createConsulContainer() {
        ServiceCreateResponse response = new ServiceCreateResponse("Consul", Constants.ContainerType.CONSUL_POOL);

        if (!this.consulContainer.find().isFound()) {
            DockerObjTemplate.CreateResponse createResponse = this.consulContainer.create();
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

    public ServiceCreateResponse createLogDatabaseContainer() {
        ServiceCreateResponse response = new ServiceCreateResponse("LogsDatabase", Constants.ContainerType.POSTGRES_DB);


        return response;
    }

}
