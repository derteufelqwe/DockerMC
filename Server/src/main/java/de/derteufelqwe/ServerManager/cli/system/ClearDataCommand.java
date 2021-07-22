package de.derteufelqwe.ServerManager.cli.system;

import com.github.dockerjava.api.exception.NotFoundException;
import com.google.inject.Inject;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.cli.SystemCmd;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.setup.infrastructure.RedisContainer;
import de.derteufelqwe.ServerManager.setup.infrastructure.RegistryCertificates;
import de.derteufelqwe.ServerManager.setup.infrastructure.RegistryContainer;
import de.derteufelqwe.ServerManager.utils.Commons;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@CommandLine.Command(name = "clearData", description = "Clears all data produces by DockerMC. THIS CANT BE UNDONE!")
@Log4j2
public class ClearDataCommand implements Runnable {

    @Inject private Docker docker;
    @Inject private Commons commons;
    @Inject private Config<MainConfig> mainConfig;

    @CommandLine.ParentCommand
    private SystemCmd parent;

    @Override
    public void run() {
        log.warn("You are about to delete every persistent storage of DockerMC. This includes all images in the registry " +
                "as well your registrys SSL certificate. The infrastructure must be stopped for this. Are you sure?");
        String input = parent.getParent().getLineReader().readLine("Enter 'Delete everything' to confirm> ");

        if (!input.equals("Delete everything")) {
            log.info("Data deletion cancelled.");
            return;
        }

        // Check if the containers are stopped
        RegistryContainer registryContainer = new RegistryContainer();
        registryContainer.init(docker, mainConfig);
        if (registryContainer.find().isFound()) {
            log.error("Registry must be stopped for full data deletion!");
            return;
        }

        RedisContainer redisContainer = new RedisContainer();
        redisContainer.init(docker, mainConfig);
        if (redisContainer.find().isFound()) {
            log.error("Redis must be stopped for full data deletion!");
            return;
        }

        RegistryCertificates certificates = new RegistryCertificates(docker, mainConfig.get());
        if (certificates.find().isFound()) {
            certificates.destroy();
        }

        try {
            docker.getDocker().removeVolumeCmd(Constants.REGISTRY_VOLUME_NAME).exec();

        } catch (NotFoundException ignored) {}


        log.info("Deleted all volumes DockerMC volumes.");
    }
}
