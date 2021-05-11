package de.derteufelqwe.ServerManager.cli.system;

import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.utils.Commons;
import de.derteufelqwe.ServerManager.utils.Utils;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@CommandLine.Command(name = "createInfrastructure", aliases = {"startInfra"}, description = "Creates the required infrastructure for DockerMC")
@Log4j2
public class CreateInfrastructureCmd implements Runnable {

    private final Commons commons = ServerManager.getCommons();

    @CommandLine.Option(names = {"-a", "--all"}, description = "Creates all parts of the infrastructure")
    private boolean all = false;

    @CommandLine.Option(names = {"-n", "--network"}, description = "Creates the overlay network")
    private boolean createNetwork = false;

    @CommandLine.Option(names = {"-rc", "--registryCerts"}, description = "Creates the SSL certificates for the registry")
    private boolean createRegistryCerts = false;

    @CommandLine.Option(names = {"-r", "--registry"}, description = "Creates the docker registry")
    private boolean createRegistry = false;

    @CommandLine.Option(names = {"-rd", "--redis"}, description = "Creates the redis container")
    private boolean createRedis = false;

    @CommandLine.Option(names = {"-nw", "--nodewatcher"}, description = "Creates the NodeWatcher service")
    private boolean createNodeWatcher = false;


    @Override
    public void run() {
        // Print help if no args are specified
        if (Utils.allFalse(all, createNetwork, createRegistryCerts, createRegistry, createRedis, createNodeWatcher)) {
            System.out.println(new CommandLine(this).getUsageMessage());
            return;
        }

        // Full setup
        if (this.all) {
            log.info("Creating full infrastructure...");
            if (commons.createFullInfrastructure()) {
                log.info("Infrastructure is up and running.");
            } else {
                log.error("Infrastructure setup failed. Solve the issues above to ensure full functionality.");
            }
            return;
        }

        // Partial setup
        if (createNetwork)
            commons.createOvernetNetwork();

        if (createRegistryCerts)
            commons.createRegistryCertificates();

        if (createRegistry)
            commons.createRegistryContainer();

        if (createRedis)
            commons.createRedisContainer();

        if (createNodeWatcher)
            commons.createNodeWatcherService();
    }
}
