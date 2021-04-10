package de.derteufelqwe.ServerManager.cli.system;

import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.utils.Commons;
import de.derteufelqwe.ServerManager.utils.Utils;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@CommandLine.Command(name = "shutdownInfrastructure", aliases = {"stopInfra"}, description = "Stops the required infrastructure for DockerMC")
@Log4j2
public class ShutdownInfrastructureCmd implements Runnable {

    private final Commons commons = ServerManager.getCommons();

    @CommandLine.Option(names = {"-a", "--all"}, description = "Stops all parts of the infrastructure")
    private boolean all = false;

    @CommandLine.Option(names = {"-r", "--registry"}, description = "Stops the docker registry")
    private boolean stopRegistry = false;

    @CommandLine.Option(names = {"-rd", "--redis"}, description = "Stops the redis container")
    private boolean stopRedis = false;

    @CommandLine.Option(names = {"-nw", "--nodewatcher"}, description = "Stops the NodeWatcher service")
    private boolean stopNodeWatcher = false;


    @Override
    public void run() {
        // Print help if no args are specified
        if (Utils.allFalse(all, stopRegistry, stopRedis, stopNodeWatcher)) {
            System.out.println(new CommandLine(this).getUsageMessage());
            return;
        }

        // Full shutdown
        if (this.all) {
            log.info("Creating full infrastructure...");
            if (commons.stopInfrastructure()) {
                log.info("Infrastructure is up and running.");
            } else {
                log.error("Infrastructure setup failed. Solve the issues above to ensure full functionality.");
            }
            return;
        }

        // Partial shutdown
        if (stopRegistry)
            commons.stopRegistryContainer();

        if (stopRedis)
            commons.stopRedisContainer();

        if (stopNodeWatcher)
            commons.stopNodeWatcherService();
    }
}
