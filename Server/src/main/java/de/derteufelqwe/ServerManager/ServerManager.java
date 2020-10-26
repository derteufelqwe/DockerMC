package de.derteufelqwe.ServerManager;

import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.api.model.ServiceSpec;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.google.common.net.HostAndPort;
import de.derteufelqwe.ServerManager.commands.*;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.RunningConfig;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.ConfigCreateResponse;
import de.derteufelqwe.ServerManager.setup.InfrastructureSetup;
import de.derteufelqwe.ServerManager.setup.ServerConfigSetup;
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
 * - Manager website
 * - Better Minecraft plugin
 * - Update services when their config changes
 * - Make config aware of changes
 */

public class ServerManager {

    private static String DOCKER_IP = "ubuntu1";
    public static Config CONFIG = new Config(new DefaultYamlConverter(), new DefaultGsonProvider(), Constants.CONFIG_PATH);

    static {
        CONFIG.registerConfig(MainConfig.class, Constants.Configs.MAIN.filename());
        CONFIG.registerConfig(RunningConfig.class, Constants.Configs.RUNNING.filename());
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
        return new InfrastructureSetup(docker).setup();
    }

    /**
     * Creates all the servers specified in the InfrastructureConfig.yml.
     * Identifies and stops lost services.
     */
    private void checkAndCreateMCServers() {
        ServerConfigSetup setup = new ServerConfigSetup(getDocker(), this.keyValueClient);
        ConfigCreateResponse response = setup.setup();
        List<Service> lostServices = setup.findLostServices(response);

        System.err.println("Found " + lostServices.size() + " lost services.");
        for (Service lostService : lostServices) {
            System.out.println("Removing lost service " + lostService.getSpec().getName() + ".");
            docker.getDocker().removeServiceCmd(lostService.getId()).exec();
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
            serverManager.checkAndCreateInfrastructure();
            serverManager.consul = Consul.builder().withHostAndPort(HostAndPort.fromParts("ubuntu1", Constants.CONSUL_PORT)).build();
            serverManager.keyValueClient = serverManager.consul.keyValueClient();

            serverManager.checkAndCreateMCServers();



        } finally {
            serverManager.onExit();
        }

    }

}
