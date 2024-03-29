package de.derteufelqwe.ServerManager.cli.service;

import com.google.inject.Inject;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.utils.Commons;
import de.derteufelqwe.ServerManager.utils.Utils;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "create", description = "Creates services")
@Log4j2
public class CreateServiceCmd implements Runnable {

    @Inject private Docker docker;
    @Inject private SessionBuilder sessionBuilder;
    @Inject private Commons commons;


    @CommandLine.Option(names = {"-a", "--all"}, description = "Create all services")
    private boolean all = false;
    @CommandLine.Option(names = {"-r", "--reloadConfig"}, description = "Reload the config before creating the services")
    private boolean reloadConfig = false;
    @CommandLine.Option(names = {"-f", "--force"}, description = "Even update the service if the config didn't change")
    private boolean force = false;
    @CommandLine.Option(names = {"-b", "--bungee"}, description = "Create the Bungee service")
    private boolean createBungee = false;
    @CommandLine.Option(names = {"-l", "--lobby"}, description = "Create the Lobby service")
    private boolean createLobby = false;
    @CommandLine.Option(names = {"-p", "--pool"}, description = "Create a pool service")
    private List<String> poolNames = new ArrayList<>();
    @CommandLine.Option(names = {"-pp", "--p-pool"}, description = "Create a persistent pool service")
    private List<String> persistentPoolNames = new ArrayList<>();
    @CommandLine.Option(names = {"-ap", "--all-pools"}, description = "Create a all pool service")
    private boolean createAllPools = false;
    @CommandLine.Option(names = {"-app", "--all-p-pools"}, description = "Create a all persistent pool service")
    private boolean createAllPersistentPools = false;


    @Override
    public void run() {
        if (Utils.allFalse(all, createBungee, createLobby, createAllPools, createAllPersistentPools) && poolNames.size() == 0 && persistentPoolNames.size() == 0) {
            System.out.println(new CommandLine(this).getUsageMessage());
            return;
        }

        // Reload the config if requested
        if (reloadConfig) {
            if (commons.reloadServerConfig()) {
                log.info("Reloaded server config.");

            } else {
                log.error("Server config reload failed. Solve the error and rerun the command.");
                return;
            }
        }

        // Remove lost services
        commons.removeLostServices();

        // Update everything at default
        if (all) {
            if (commons.createAllMCServers(force)) {
                log.info("Successfully created all services.");

            } else {
                log.error("Service creation failed!");
            }

            return;
        }

        // Handle the special cases
        if (createBungee)
            commons.createBungeeServer(force);

        if (createLobby)
            commons.createLobbyServer(force);

        if (createAllPools) {
            commons.createAllPoolServers(force);

        } else {
            for (String poolName : poolNames) {
                commons.createPoolServer(poolName, force);
            }
        }

        if (createAllPersistentPools) {
            commons.createAllPersistentPoolServers(force);

        } else {
            for (String poolName : persistentPoolNames) {
                commons.createPersistentPoolServer(poolName, force);
            }
        }

    }

}
