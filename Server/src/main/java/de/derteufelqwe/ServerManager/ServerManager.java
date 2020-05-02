package de.derteufelqwe.ServerManager;

import com.github.dockerjava.api.model.ContainerSpec;
import com.github.dockerjava.api.model.ServiceSpec;
import com.github.dockerjava.api.model.TaskSpec;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.google.common.net.HostAndPort;
import de.derteufelqwe.ServerManager.commands.*;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.RunningConfig;
import de.derteufelqwe.ServerManager.config.backend.Config;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.infrastructure.CertificateCreator;
import de.derteufelqwe.ServerManager.setup.infrastructure.ConsulService;
import de.derteufelqwe.ServerManager.setup.infrastructure.NginxService;
import de.derteufelqwe.ServerManager.setup.infrastructure.RegistryContainer;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;
import lombok.Getter;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ServerManager {

    static {
        Config.registerConfig(MainConfig.class, "", Constants.Configs.MAIN.filename());
        Config.registerConfig(RunningConfig.class, "", Constants.Configs.RUNNING.filename());
        Config.registerConfig(InfrastructureConfig.class, "", Constants.Configs.INFRASTRUCTURE.filename());
        Config.loadAll();
    }

    @Getter
    private static Docker docker = new Docker("tcp", "192.168.178.28", 2375);
    private Consul consul;
    public KeyValueClient keyValueClient;

    public ServerManager() {
        this.consul = Consul.builder().withHostAndPort(HostAndPort.fromParts("ubuntu1", 8500)).build();
        this.keyValueClient = consul.keyValueClient();
    }


    public void onStart() {
//        System.out.println(de.derteufelqwe.commons.Constants.LOGO);
//        System.out.println("> Developed by " + de.derteufelqwe.commons.Constants.AUTHOR + ".\n");
        if (!this.checkAndCreateInfrastructure()) {
            throw new FatalDockerMCError("System setup failed. Resolve the issues to continue.");
        }

    }


    public void onExit() throws Exception {
        System.out.println("Saving config...");
        Config.saveAll();

        // Required for Docker-Java to fully quit the execution. Will block otherwise.
        docker.getDocker().close();
    }


    /**
     * Checks if the required infrastructure exist and creates it if necessary.
     */
    private boolean checkAndCreateInfrastructure() {
        CertificateCreator certCreator = new CertificateCreator();
        int serviceCount = 8;
        int failedSetups = 0;

        System.out.println("Checking and setting up infrastructure...");

        // Registry certificates
        if (!certCreator.findRegistryCerts()) {
            System.out.println("Couldn't find required certificates for the registry. Creating them...");
            certCreator.generateRegistryCerts(false);

            if (certCreator.findRegistryCerts()) {
                System.out.println("Successfully generated the required certificates for the registry.");

            } else {
                System.err.println("Couldn't generate the required certificates for the registry.");
                failedSetups++;
            }
        } else {
            System.out.println("Found existing certificates for the registry.");
        }

        System.out.println(String.format("Successfully set %s/%s services.", serviceCount - failedSetups, serviceCount));
        if (failedSetups != 0)
            System.err.println(String.format("%s services failed to start. Fix the errors before you proceed.", failedSetups));

        return failedSetups == 0;
    }

    /**
     * ToDo: Save logs when logger is added
     * Creates all the servers specified in the InfrastructureConfig.yml.
     *
     * @return Successfully created all server or not
     */
    private boolean checkAndCreateMCServers() {
        int successfulStarts = 0;
        int failedStarts = 0;


        System.out.println(String.format("Successfully started %s / %s services.", successfulStarts, successfulStarts + failedStarts));
        return failedStarts == 0 && successfulStarts > 0;
    }


    /**
     * Handles the shutdown of the network. Will stop or kill all containers if requested.
     *
     * @param killContainers Should containers be killed on stop?
     */
    private void requestShutdown(boolean killContainers) {

    }


    public void startCommandDispatcher() {
        System.out.println("Enter help for help.");
        Scanner scanner = new Scanner(System.in);

        whileloop:
        while (true) {
            System.out.print("> ");
            List<String> input = Utils.splitArgString(scanner.nextLine());

            if (input.size() == 0)
                continue;

            if (input.get(0).equals(""))    // Empty input
                continue;

            String command = input.get(0);
            String[] inputArray = input.subList(1, input.size()).toArray(new String[0]);
            int exitCode = 0;


            switch (command.toLowerCase()) {
                case "exit":
                    ExitCommand exitCommand = new ExitCommand();
                    exitCode = new CommandLine(exitCommand).execute(inputArray);

                    if (exitCode == CommandLine.ExitCode.OK) {
                        this.requestShutdown(exitCommand.isKillContainers());
                        break whileloop;
                    }
                    break;

                case "status":
                    exitCode = new CommandLine(new StatusCommand()).execute(inputArray);
                    break;

                case "docker":
                    exitCode = new CommandLine(new DockerCommand()).execute(inputArray);
                    break;

                case "system":
                    exitCode = new CommandLine(new SystemCmd()).execute(inputArray);
                    break;

                case "images":
                    exitCode = new CommandLine(new ImageCmd()).execute(inputArray);
                    break;

                case "help":
                default:
                    exitCode = new CommandLine(new HelpCommand()).execute(inputArray);
                    break;
            }


        }

        System.out.println("Goodbye.");
    }


    private void killServices() {

    }

    /*
     * - Refactor the base containers to have a single class for each container to find, create and destroy it
     *
     */

    public static void main(String[] args) throws Exception {
        ServerManager serverManager = new ServerManager();

        try {
//            ConsulService consulService = new ConsulService(docker);
//            System.out.println(consulService.create());

/**/

            serverManager.keyValueClient.putValue("system/lobbyServerName", "Lobby");
            serverManager.keyValueClient.putValue("mcservers/Lobby/softPlayerLimit", "2");

/**/

//            System.out.println("Starting nginx Service");
            NginxService nginxService = new NginxService("mcproxy", "512M", "1", "NginxProxy", 2,
                    new ServiceConstraints(1), 25577);
            nginxService.init(docker);
            nginxService.create();
//
//            BungeePool bungeePool = new BungeePool("waterfall", "512M", "1", "BungeeCord", 2, new ServiceConstraints(1));
//            bungeePool.init(docker);
//            bungeePool.create();
//
//            ServerPool lobbyPool = new ServerPool("testmc", "512M", "1", "MyLobby", 2, null, 5);
//            lobbyPool.init(docker);
//            System.out.println(lobbyPool.create());
//
//            ServerPool serverPool = new ServerPool("testmc", "512M", "1", "Minigame-1", 2, null, 2);
//            serverPool.init(docker);
//            System.out.println(serverPool.create());
//
//            ServerPool serverPool2 = new ServerPool("testmc", "512M", "1", "Minigame-2", 2, null, 2);
//            serverPool2.init(docker);
//            System.out.println(serverPool2.create());


        } finally {
            serverManager.onExit();
        }

    }

}

/*
 * @ArgGroup(validate=false, description="sdf") -> Text in Helpmessage
 * Argument: order -> Order setzen
 * Implement Callable<Class> statt Runnable für custom Returntype
 */
