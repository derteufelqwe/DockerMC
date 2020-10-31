package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.infrastructure.ConsulService;
import de.derteufelqwe.ServerManager.setup.infrastructure.RegistryCertificates;
import de.derteufelqwe.ServerManager.setup.infrastructure.RegistryContainer;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.commons.Constants;

/**
 * Class to create the default infrastructure
 */
public class InfrastructureSetup {

    private Docker docker;
    private RegistryCertificates registryCertificates;
    private RegistryContainer registryContainer;
    private ConsulService consulService;


    public InfrastructureSetup(Docker docker) {
        this.docker = docker;
        this.registryCertificates = new RegistryCertificates(docker);
        this.registryContainer = new RegistryContainer();
        this.registryContainer.init(docker);
        this.consulService = new ConsulService();
        this.consulService.init(docker);
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

    public ServiceCreateResponse createConsulService() {
        ServiceCreateResponse response = new ServiceCreateResponse("Consul", Constants.ContainerType.CONSUL_POOL);

        if (!this.consulService.find().isFound()) {
            DockerObjTemplate.CreateResponse createResponse = this.consulService.create();
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
