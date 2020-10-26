package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.infrastructure.ConsulService;
import de.derteufelqwe.ServerManager.setup.infrastructure.RegistryCertificates;
import de.derteufelqwe.ServerManager.setup.infrastructure.RegistryContainer;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;

/**
 * Class to create the default infrastructure
 */
public class InfrastructureSetup {

    private Docker docker;
    private RegistryCertificates registryCertificates;
    private RegistryContainer registryContainer;
    private ConsulService consulService;

    private int failedSetups = 0;
    private int serviceCount = 3;


    public InfrastructureSetup(Docker docker) {
        this.docker = docker;
        this.registryCertificates = new RegistryCertificates(docker);
        this.registryContainer = new RegistryContainer();
        this.registryContainer.init(docker);
        this.consulService = new ConsulService();
        this.consulService.init(docker);
    }

    /**
     * Setups the default infrastructure
     * @return True if everything was successful, otherwise false.
     */
    public boolean setup() {
        this.failedSetups = 0;

        this.createRegistryCerts();
        this.createRegistryContainer();
        this.createConsulService();

        System.out.println(String.format("Successfully set %s/%s services.", serviceCount - failedSetups, serviceCount));
        if (failedSetups != 0)
            System.err.println(String.format("%s services failed to start. Fix the errors before you proceed.", failedSetups));

        return failedSetups == 0;
    }


    private void createRegistryCerts() {
        if (!this.registryCertificates.find().isFound()) {
            System.out.println("Couldn't find required certificates for the registry. Creating them...");
            this.registryCertificates.create();

            if (this.registryCertificates.find().isFound()) {
                System.out.println("Successfully generated the required certificates for the registry.");

            } else {
                System.err.println("Couldn't generate the required certificates for the registry.");
                this.failedSetups++;
            }
        } else {
            System.out.println("Found existing certificates for the registry.");
        }
    }

    private void createRegistryContainer() {
        if (!this.registryContainer.find().isFound()) {
            System.out.println("Couldn't find registry container. Creating it...");

            DockerObjTemplate.CreateResponse createResponse = this.registryContainer.create();
            if (createResponse.isCreated()) {
                System.out.println("Successfully created registry container.");

            } else {
                System.err.println("Failed to create registry container.");
                System.err.println(createResponse.getMessage());
                this.failedSetups++;
            }

        } else {
            System.out.println("Found existing registry container.");
        }
    }

    private void createConsulService() {
        if (!this.consulService.find().isFound()) {
            System.out.println("Couldn't find consul service. Creating it...");

            if (this.consulService.create().isCreated()) {
                System.out.println("Successfully created consul service.");

            } else {
                System.err.println("Failed to create consul service.");
                this.failedSetups++;
            }

        } else {
            System.out.println("Found existing consul service.");
        }
    }

}
