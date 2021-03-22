package de.derteufelqwe.ServerManager.spring.commands;

import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.setup.infrastructure.PostgresDBContainer;
import de.derteufelqwe.ServerManager.setup.infrastructure.RedisContainer;
import de.derteufelqwe.ServerManager.setup.infrastructure.RegistryContainer;
import de.derteufelqwe.ServerManager.spring.Commons;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.ServerManager.utils.HelpBuilder;
import de.derteufelqwe.ServerManager.utils.Utils;
import de.derteufelqwe.commons.Constants;
import lombok.extern.log4j.Log4j2;
import org.jline.reader.LineReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import sun.security.x509.X509CertImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;

@ShellComponent
@Log4j2
public class SystemCommands {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private Docker docker;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired @Lazy
    private LineReader lineReader;
    @Autowired
    private Commons commons;


    @ShellMethod(value = "Shows the help", key = "system")
    private void showHelp() {
        System.out.println("Manage the DockerMC system");
        System.out.println("");

        new HelpBuilder("Commands:")
                .addEntry("help", "Shows this help")
                .addEntry("reloadConfig", "Reloads all config files")
                .addEntry("checkInfrastructure", "Checks if the general infrastructure is up and running")
                .addEntry("registryCertInfos", "Displays information about the registry SSL certificate")
                .addEntry("shutdownInfrastructure", "Stops all infrastructure containers")
                .addEntry("cleanAllData", "Deletes ALL data gathered by DockerMC")
                .addEntry("listNodes", "Lists all available nodes in the swarm")
                .addEntry("joinNode", "Displays information on how to join a new node to the swarm")
                .print();
    }

    @ShellMethod(value = "Shows the help", key = "system help")
    private void showHelp2() {
        showHelp();
    }


    @ShellMethod(value = "Reloads all config files", key = "system reloadConfig")
    public void reloadConfig() {
        ServerManager.MAIN_CONFIG.load();
        ServerManager.SERVERS_CONFIG.load();
        log.info("Reloaded config files.");
    }

    @ShellMethod(value = "Checks if all parts of the infrastructure are up and running or starts them.", key = "system checkInfrastructure")
    public void checkInfrastructure() {
        if (commons.createFullInfrastructure()) {
            log.info("Infrastructure is up and running.");

        } else {
            log.error("Infrastructure setup failed. Solve the issues above to ensure full functionality.");
        }
    }

    @ShellMethod(value = "Prints information about the registry cert information. Most notably its expiration date.", key = "system registryCertInfos")
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
            log.info(String.format("Path         : %s", "'/etc/docker/certs.d/" + Constants.REGISTRY_URL + "'"));

        } catch (IOException e) {
            log.warn("Certificate not found! Make sure it's generated.");

        } catch (CertificateException e) {
            log.error("Invalid certificate. Try to regenerate it. Error: {}.", e.getMessage());
        }
    }

    @ShellMethod(value = "Stops all infrastructure containers.", key = {"system shutdownInfrastructure"})
    public void shutdownInfrastructure(
            @ShellOption({"-a", "--all"}) boolean all,
            @ShellOption({"-r", "--registry"}) boolean stopRegistry,
            @ShellOption({"-rd", "--redis"}) boolean stopRedis,
            @ShellOption({"-p", "--postgres"}) boolean stopPostgres,
            @ShellOption({"-nw", "--nodewatcher"}) boolean stopNodeWatcher
    ) {
        // Default action
        if (all) {
            log.warn("You are about to stop ALL Infrastructure containers. The Minecraft and BungeeCord containers depend on " +
                    "them and will stop working properly when doing so. Are you sure? (Y/N)");
            String input = lineReader.readLine("> ").toUpperCase();

            if (!input.equals("Y")) {
                log.info("System shutdown cancelled.");
                return;
            }

            if (!this.commons.stopInfrastructure()) {
                log.error("Infrastructure shutdown failed!");
            }

            return;
        }


        // Stop single services
        if (stopRegistry)
            this.commons.stopRegistryContainer();

        if (stopRedis)
            this.commons.stopRedisContainer();

        if (stopPostgres)
            this.commons.stopPostgresContainer();

        if (stopNodeWatcher)
            this.commons.stopNodeWatcherService();
    }

    @ShellMethod(value = "Clears all data that has been gathered by the infrastructure. This includes THE WHOLE DATABASE AND REGISTRY!", key = "system cleanAllData")
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

        PostgresDBContainer postgresDBContainer = new PostgresDBContainer();
        postgresDBContainer.init(docker);
        if (postgresDBContainer.find().isFound()) {
            log.error("Postgres must be stopped for full data deletion!");
            return;
        }

        try {
            docker.getDocker().removeVolumeCmd(Constants.REGISTRY_VOLUME_NAME).exec();

        } catch (NotFoundException ignored) {}

        try {
            docker.getDocker().removeVolumeCmd(Constants.POSTGRES_VOLUME_NAME).exec();

        } catch (NotFoundException ignored) {}

        log.info("Deleted all volumes DockerMC volumes.");
    }

    @ShellMethod(value = "Lists all available docker nodes", key = "system listNodes")
    public void listNodes() {
        Info info = docker.getDocker().infoCmd().exec();
        String currentNodeId = info.getSwarm().getNodeID();
        List<SwarmNode> nodes = docker.getDocker().listSwarmNodesCmd().exec();

        TableBuilder tableBuilder = new TableBuilder()
                .withColumn(new Column.Builder()
                        .withTitle("NodeID")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Name")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Status")
                        .withMinWidth(8)
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("IP")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Role")
                        .build())
                ;

        for (SwarmNode node : nodes) {
            String idPrefix = node.getId().equals(currentNodeId) ? "> " : "";
            Map<String, String> labels = node.getSpec().getLabels();

            tableBuilder.addToColumn(0, idPrefix + node.getId());
            tableBuilder.addToColumn(1, labels.getOrDefault("name", "<NO_NAME>"));
            tableBuilder.addToColumn(2, node.getSpec().getAvailability().name());
            tableBuilder.addToColumn(3, node.getStatus().getAddress());
            tableBuilder.addToColumn(4, node.getSpec().getRole().name());
        }

        tableBuilder.build(log);
    }

    @ShellMethod(value = "Returns the required information to join a new node to the swarm", key = "system joinNode")
    public void joinNode() {
        Swarm swarm = docker.getDocker().inspectSwarmCmd().exec();
        SwarmInfo swarmInfo = docker.getDocker().infoCmd().exec().getSwarm();

        log.info("To join a new node to the swarm enter the following command on the host you want to join.");
        log.info("docker swarm join --token {} {}:2377", swarm.getJoinTokens().getWorker(), swarmInfo.getNodeAddr());

        log.info("Afterwards you can add a name to your node if you wish using the following command. (Replace NAME and NODE_ID with the fitting values)");
        log.info("docker node update --label-add name=<NAME> <NODE_ID>");
    }


}
