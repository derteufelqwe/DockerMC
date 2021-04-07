package de.derteufelqwe.ServerManager.cli.service;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.cli.ServiceCmd;
import de.derteufelqwe.ServerManager.spring.Commons;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "stop", description = "Stops running services")
@Log4j2
public class StopServiceCmd implements Runnable {

    private Docker docker = ServerManager.getDocker();
    private SessionBuilder sessionBuilder = ServerManager.getSessionBuilder();
    private Commons commons = ServerManager.getCommons();

    @CommandLine.ParentCommand
    private ServiceCmd parent;

    @CommandLine.Option(names = {"-a", "--all"}, description = "Stop all services")
    private boolean all = false;
    @CommandLine.Option(names = {"-b", "--bungee"}, description = "Stop the Bungee service")
    private boolean stopBungee = false;
    @CommandLine.Option(names = {"-l", "--lobby"}, description = "Stop the Lobby service")
    private boolean stopLobby = false;
    @CommandLine.Option(names = {"-p", "--pool"}, description = "Stop a pool service")
    private List<String> poolNames = new ArrayList<>();


    @Override
    public void run() {
        if (all) {
            log.warn("You are about to stop ALL Minecraft and BungeeCord servers, kicking all players in the process. Are you sure? (Y/N)");
            String input = parent.getParent().getLineReader().readLine("> ").toUpperCase();

            if (!input.equals("Y")) {
                log.info("Server shutdown cancelled.");
                return;
            }

            if (commons.stopAllMCServers()) {
                log.info("Successfully stopped all Minecraft and BungeeCord services.");

            } else {
                log.error("Stopping all Minecraft / BungeeCord servers failed.");
            }
            return;
        }

        // Specific actions
        if (stopBungee)
            commons.stopBungeeServer();

        if (stopLobby)
            commons.stopLobbyServer();

        for (String poolName : poolNames) {
            commons.stopPoolServer(poolName);
        }
    }

}
