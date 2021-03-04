package de.derteufelqwe.ServerManager.spring.commands;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Service;
import com.sun.javaws.exceptions.InvalidArgumentException;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.setup.infrastructure.PostgresDBContainer;
import de.derteufelqwe.ServerManager.setup.infrastructure.RedisContainer;
import de.derteufelqwe.ServerManager.setup.infrastructure.RegistryContainer;
import de.derteufelqwe.ServerManager.spring.events.CheckInfrastructureEvent;
import de.derteufelqwe.ServerManager.spring.events.ReloadConfigEvent;
import de.derteufelqwe.commons.Constants;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.jline.reader.LineReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiColors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import sun.security.x509.X509CertImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

@ShellComponent
@Log4j2
@ShellCommandGroup(value = "system")
public class SystemCommands {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private Docker docker;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired @Lazy
    private LineReader lineReader;


    @ShellMethod(value = "Reloads all config files", key = "system reload-config")
    public void reloadConfig() {
        ServerManager.MAIN_CONFIG.load();
        ServerManager.SERVERS_CONFIG.load();
        log.info("Reloaded config files.");
    }

    @ShellMethod(value = "Reloads and updates the servers config.", key = "system check-servers")
    public void checkServers() {
        ReloadConfigEvent reloadConfigEvent = new ReloadConfigEvent(this, ReloadConfigEvent.ReloadSource.COMMAND);
        applicationEventPublisher.publishEvent(reloadConfigEvent);

        if (reloadConfigEvent.isSuccess()) {
            log.info("Successfully reloaded server config.");

        } else {
            log.error("Config reload failed with: '{}'", reloadConfigEvent.getMessage());
        }
    }

    @ShellMethod(value = "Checks if all parts of the infrastructure are up and running or starts them.", key = "system check-infrastructure")
    public void checkInfrastructure() {
        CheckInfrastructureEvent infrastructureEvent = new CheckInfrastructureEvent(this, CheckInfrastructureEvent.ReloadSource.COMMAND);
        applicationEventPublisher.publishEvent(infrastructureEvent);

        if (infrastructureEvent.isSuccess()) {
            log.info("Infrastructure is up and running.");

        } else {
            log.error("Infrastructure setup failed. Solve the issues above to ensure full functionality.");
        }
    }

    @ShellMethod(value = "Prints information about the registry cert information. Most notably its expiration date.", key = "system registry-cert-infos")
    public void registryCertInfos() {
        try {
            File certFile = new File(Constants.REGISTRY_CERT_PATH_1 + Constants.REGISTRY_CERT_NAME);
            X509Certificate certificate = new X509CertImpl(new FileInputStream(certFile));

            log.info("SSL certificate information:");
            log.info(String.format("Name         : %s", Constants.REGISTRY_CERT_NAME));
            log.info(String.format("Start        : %s", certificate.getNotBefore().toString()));
            if (certificate.getNotAfter().before(new Date(System.currentTimeMillis())))
                log.error(String.format("Expiration   : %s (Expired)", certificate.getNotAfter().toString()));
            else
                log.info(String.format("Expiration   : %s", certificate.getNotAfter().toString()));
            log.info(String.format("Serialnumber : %s", certificate.getSerialNumber().toString()));

        } catch (IOException e) {
            log.warn("Certificate not found! Make sure it's generated.");

        } catch (CertificateException e) {
            log.error("Invalid certificate. Try to regenerate it. Error: {}.", e.getMessage());
        }
    }

    @ShellMethod(value = "Stops ALL Minecraft and BungeeCord server.", key = {"system shutdown-servers"})
    public void shutdownServer() {
        log.warn("You are about to stop ALL Minecraft and BungeeCord server, kicking all players in the process. Are you sure? (Y/N)");
        String input = lineReader.readLine("> ").toUpperCase();

        if (!input.equals("Y")) {
            log.info("Server shutdown cancelled.");
            return;
        }

        List<Service> services = docker.getDocker().listServicesCmd()
                .withLabelFilter(Constants.DOCKER_IDENTIFIER_MAP)
                .exec();
        log.info("Found {} active services.", services.size());

        for (Service service : services) {
            String name = service.getSpec().getName();
            String type = service.getSpec().getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY);

            docker.getDocker().removeServiceCmd(service.getId()).exec();
            log.info("Removed {} service {} ({}).", type, name, service.getId());
        }

        log.info("Successfully stopped all Minecraft and BungeeCord services.");
    }

    @ShellMethod(value = "Stops all infrastructure container..", key = {"system shutdown-infrastructure"})
    public void shutdownInfrastructure() {
        log.warn("You are about to stop ALL Infrastructure containers. The Minecraft and BungeeCord containers depend on " +
                "them and will stop working properly when doing so. Are you sure? (Y/N)");
        String input = lineReader.readLine("> ").toUpperCase();

        if (!input.equals("Y")) {
            log.info("System shutdown cancelled.");
            return;
        }

        List<Container> containers = docker.getDocker().listContainersCmd()
                .withLabelFilter(Constants.DOCKER_IDENTIFIER_MAP)
                .exec();

        for (Container container : containers) {
            String typeStr = container.getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY);
            if (typeStr == null)
                continue;

            try {
                Constants.ContainerType type = Constants.ContainerType.valueOf(typeStr);

                switch (type) {
                    case REGISTRY:
                        docker.getDocker().stopContainerCmd(container.getId()).exec();
                        log.info("Stopped registry container.");
                        break;

                    case REDIS_DB:
                        docker.getDocker().stopContainerCmd(container.getId()).exec();
                        log.info("Stopped redis container");
                        break;

                    case POSTGRES_DB:
                        log.info("Stopped postgres container.");
                        docker.getDocker().stopContainerCmd(container.getId()).exec();
                        break;
                }

            } catch (IllegalArgumentException e) {

            }
        }

        log.info("Successfully stopped all infrastructure containers.");
    }

    @ShellMethod(value = "Clears all data that has been gathered by the infrastructure. This includes THE WHOLE DATABASE AND REGISTRY!", key = "system clean-all-data")
    public void clearData() {
        log.warn("You are about to delete every persistent storage of DockerMC. This includes all images in the registry " +
                "as well as the whole PostgreSQL database. The infrastructure must be stopped for this. Are you sure?");
        String input = lineReader.readLine("Enter 'Delete everything' to confirm> ");

        if (!input.equals("Delete everything")) {
            log.info("Data deletion cancelled.");
            return;
        }

        // Check if the containers are stopped
        RegistryContainer registryContainer = new RegistryContainer();
        registryContainer.init(docker);
        if (registryContainer.find().isFound()) {
            log.error("Registry must be stopped for full data deletion!");
            return;
        }

        RedisContainer redisContainer = new RedisContainer();
        redisContainer.init(docker);
        if (redisContainer.find().isFound()) {
            log.error("Redis must be stopped for full data deletion!");
            return;
        }

    }

}
