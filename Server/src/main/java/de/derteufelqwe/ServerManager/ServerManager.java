package de.derteufelqwe.ServerManager;

import com.github.dockerjava.api.model.Service;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.google.common.net.HostAndPort;
import de.derteufelqwe.ServerManager.commands.*;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.SystemConfig;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.*;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import lombok.Getter;
import picocli.CommandLine;

import java.util.List;
import java.util.Scanner;


/**
 * Program flow:
 * - Create required infrastructure
 * - Validate config
 * - Create servers based on config
 */

/**
 * Problems:
 * - BungeeCord servers are not synced
 */

/**
 * ToDos:
 * - CLI
 *   - Setup the server
 *   - Change server config
 *   - Get status information
 *   - Create images
 * - Logger
 * - Manager website
 * - Better Minecraft plugin
 *   - Block Players from entering a certain server, to update it
 * - Update services when their config changes
 * - Make config aware of changes
 * - API for Bungeecord
 * - Config checker
 */

public class ServerManager {

    private static String DOCKER_IP = "ubuntu1";
    public static Config CONFIG = new Config(new DefaultYamlConverter(), new DefaultGsonProvider(), Constants.CONFIG_PATH);

    static {
        CONFIG.registerConfig(MainConfig.class, Constants.Configs.MAIN.filename());
        CONFIG.registerConfig(SystemConfig.class, Constants.Configs.SYSTEM.filename());
        CONFIG.registerConfig(InfrastructureConfig.class, Constants.Configs.INFRASTRUCTURE.filename());
        CONFIG.loadAll();
    }

    @Getter
    private static Docker docker = new Docker("tcp", DOCKER_IP, 2375);
    private Consul consul;
    public KeyValueClient keyValueClient;

    public ServerManager() {

    }


    public void onStart() {
//        System.out.println(de.derteufelqwe.commons.Constants.LOGO);
//        System.out.println("> Developed by " + de.derteufelqwe.commons.Constants.AUTHOR + ".\n");
        if (!this.checkAndCreateInfrastructure()) {
            throw new FatalDockerMCError("System setup failed. Resolve the issues to continue.");
        }

    }


    public void onExit() throws Exception {
        // Required for Docker-Java to fully quit the execution. Will block otherwise.
        docker.getDocker().close();

        System.out.println("Saving config...");
        CONFIG.saveAll();
    }


    /**
     * Checks if the required infrastructure exist and creates it if necessary.
     */
    private boolean checkAndCreateInfrastructure() {
        InfrastructureSetup setup = new InfrastructureSetup(docker);

        // Registry Certificates
//        System.out.println("Creating required certificates for the registry.");
        ServiceCreateResponse response1 = setup.createRegistryCerts();
        switch (response1.getResult()) {
            case OK:
                System.out.println("Created registry certificates successfully."); break;
            case RUNNING:
                System.out.println("Registry certificates already existing."); break;
            case FAILED_GENERIC:
                System.err.printf("Failed to create the registry certificates! Message: %s.",
                        response1.getAdditionalInfos()); break;
        }

        // Registry Container
//        System.out.println("Starting the registry container.");
        ServiceCreateResponse response2 = setup.createRegistryContainer();
        switch (response2.getResult()) {
            case OK:
                System.out.println("Created registry container successfully."); break;
            case RUNNING:
                System.out.println("Registry container already running."); break;
            case FAILED_GENERIC:
                System.err.printf("Failed to create the registry container! ID: %s, Message: %s.%n",
                        response2.getServiceId(), response2.getAdditionalInfos()); break;
        }

        // Consul Service
//        System.out.println("Starting the Consul service..");
        ServiceCreateResponse response3 = setup.createConsulService();
        switch (response3.getResult()) {
            case OK:
                System.out.println("Created Consul service successfully."); break;
            case RUNNING:
                System.out.println("Consul service already running."); break;
            case FAILED_GENERIC:
                System.err.printf("Failed to create the Consul service! ID: %s, Message: %s.%n",
                        response3.getServiceId(), response3.getAdditionalInfos()); break;
        }

        return true;
    }

    /**
     * Creates all the servers specified in the InfrastructureConfig.yml.
     * Identifies and stops lost services.
     */
    private void checkAndCreateMCServers() {
        LostServiceCleaner cleaner = new LostServiceCleaner(docker);
        List<Service> lostServices = cleaner.findLostServices();

        for (Service lostService : lostServices) {
            System.out.printf("Removing lost service %s (%s).", lostService.getId(), lostService.getSpec().getName());
            docker.getDocker().removeServiceCmd(lostService.getId()).exec();
        }


        MCServerConfigSetup setup = new MCServerConfigSetup(getDocker(), this.keyValueClient);

        // BungeeCord
//        System.out.println("Creating BungeeCord-Pool service.");
        ServiceCreateResponse response1 = setup.createBungeePool();
        switch (response1.getResult()) {
            case OK:
                System.out.println("BungeeCord-Pool created successfully."); break;
            case RUNNING:
                System.out.println("BungeeCord-Pool already running."); break;
            case NOT_CONFIGURED:
                System.err.println("BungeeCord-Pool not configured."); break;
            case FAILED_GENERIC:
                System.err.printf("Failed to create the BungeeCord-Pool. ServiceId: %s, Message: %s.",
                        response1.getServiceId(), response1.getAdditionalInfos()); break;
        }

        // Lobby Pool
//        System.out.println("Creating BungeeCord-Pool service.");
        ServiceCreateResponse response2 = setup.createLobbyPool();
        switch (response2.getResult()) {
            case OK:
                System.out.println("LobbyServer-Pool created successfully."); break;
            case RUNNING:
                System.out.println("LobbyServer-Pool already running."); break;
            case NOT_CONFIGURED:
                System.err.println("LobbyServer-Pool not configured."); break;
            case FAILED_GENERIC:
                System.err.printf("Failed to create the LobbyServer-Pool. ServiceId: %s, Message: %s.",
                        response2.getServiceId(), response2.getAdditionalInfos()); break;
        }

        // Other Server Pools
//        System.out.println("Creating other MinecraftServer-Pools.");
        for (ServiceCreateResponse response3 : setup.createPoolServers()) {
            switch (response3.getResult()) {
                case OK:
                    System.out.println("MinecraftServer-Pool created successfully."); break;
                case RUNNING:
                    System.out.println("MinecraftServer-Pool already running."); break;
                case NOT_CONFIGURED:
                    System.err.println("MinecraftServer-Pool not configured."); break;
                case FAILED_GENERIC:
                    System.err.printf("Failed to create the MinecraftServer-Pool. ServiceId: %s, Message: %s.",
                            response3.getServiceId(), response3.getAdditionalInfos()); break;
            }
        }

        // ToDo: Handle invalid setup
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


    public static void main(String[] args) throws Exception {
        ServerManager serverManager = new ServerManager();

        try {
//            serverManager.checkAndCreateInfrastructure();
            serverManager.consul = Consul.builder().withHostAndPort(HostAndPort.fromParts("ubuntu1", Constants.CONSUL_PORT)).build();
            serverManager.keyValueClient = serverManager.consul.keyValueClient();

//            serverManager.checkAndCreateMCServers();

            ServerConfigUpdater updater = new ServerConfigUpdater(docker, serverManager.keyValueClient);
            ServiceUpdateResponse response = updater.updateBungeePool();
            System.out.println(response.getResult());

            String id = "8f8spyj76qh0";
//            Service service = docker.getDocker().inspectServiceCmd(id).exec();
//            ServiceSpec spec = service.getSpec();
//
//            spec.getTaskTemplate().getContainerSpec().getEnv().remove(2);
//            spec.getTaskTemplate().getContainerSpec().getEnv().add("SOFT_PLAYER_LIMIT=6");
//            spec.getTaskTemplate().withForceUpdate(2);
//            spec.withUpdateConfig(new UpdateConfig().withParallelism(0L));
//
//            docker.getDocker().updateServiceCmd(id, spec).withVersion(service.getVersion().getIndex()).exec();
            return;

        } finally {
            serverManager.onExit();
        }

    }

}
