package de.derteufelqwe.ServerManager;

import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.commands.*;
import de.derteufelqwe.ServerManager.config.Config;
import de.derteufelqwe.ServerManager.config.configs.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.configs.MainConfig;
import de.derteufelqwe.ServerManager.config.configs.RunningConfig;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.BaseContainerCreator;
import de.derteufelqwe.ServerManager.setup.CertificateCreator;
import de.derteufelqwe.ServerManager.setup.LobbyPoolCreator;
import de.derteufelqwe.commons.Constants;
import lombok.Getter;
import picocli.CommandLine;

import java.util.List;
import java.util.Scanner;

public class ServerManager {

    static {
        Config.registerConfig(MainConfig.class, "", "MainConfig.yml");
        Config.registerConfig(RunningConfig.class, "", "RunningConfig.yml");
        Config.registerConfig(InfrastructureConfig.class, "", "InfrastructureConfig.yml");
        Config.loadAll();
    }

    @Getter
    private static Docker docker = new Docker();
    @Getter
    private static Logger logger = new Logger();


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
        Config.saveAll();

        // Required for Docker-Java to fully quit the execution. Will block otherwise.
        docker.getDocker().close();
    }


    /**
     * Checks if the required infrastructure exist and creates it if necessary.
     */
    private boolean checkAndCreateInfrastructure() {
        BaseContainerCreator validator = new BaseContainerCreator();
        CertificateCreator creator = new CertificateCreator();
        boolean removeOldDns = true;
        boolean createProxyCerts = false;
        int serviceCount = 7;
        int failedSetups = 0;

        System.out.println("Checking and setting up infrastructure...");

        // 1 - Overnet network
        if (!validator.findNetworkOvernet()) {
            System.out.println(String.format("Failed to find network %s. Creating it...", Constants.NETW_OVERNET_NAME));
            if (validator.createNetworkOvernet()) {
                System.out.println(String.format("Successfully created network %s.", Constants.NETW_OVERNET_NAME));

            } else {
                System.err.println(String.format("Failed to create network %s.", Constants.NETW_OVERNET_NAME));
                failedSetups++;
            }
        } else {
            System.out.println(String.format("Found existing network %s.", Constants.NETW_OVERNET_NAME));
        }

        // 2 - API_net network
        if (!validator.findNetworkApiNet()) {
            System.out.println(String.format("Failed to find network %s. Creating it...", Constants.NETW_API_NAME));
            if (validator.createNetworkApiNet()) {
                System.out.println(String.format("Successfully created network %s.", Constants.NETW_API_NAME));

            } else {
                System.err.println(String.format("Failed to create network %s.", Constants.NETW_API_NAME));
                failedSetups++;
            }
        } else {
            System.out.println(String.format("Found existing network %s.", Constants.NETW_API_NAME));
        }

        // 3 - API-proxy certificates
        if (!creator.findAPIProxyCerts()) {
            System.out.println("Couldn't find required certificates for the API-proxy. Generating them...");
            creator.generateAPIProxyCerts(false);

            if (creator.findAPIProxyCerts()) {
                System.out.println("Successfully generated the certificates for the API-proxy.");

            } else {
                System.err.println("Failed to create the API-proxy certificates.");
                failedSetups++;
            }
        } else {
            System.out.println("Found existing API-proxy certificates.");
        }

        // 4 - API-proxy container
        if (!validator.findAPIProxy()) {
            System.out.println("Failed to find API-proxy container. Creating it...");
            if (validator.createAPIProxy(createProxyCerts)) {
                System.out.println("Successfully created API-proxy container.");

            } else {
                System.err.println("Failed to create API-proxy container.");
                failedSetups++;
            }
        } else {
            System.out.println("Found existing API-proxy container.");
        }

        // 5 - DNS container
        if (!validator.findDns()) {
            System.out.println("Failed to find DNS container. Creating it...");
            if (validator.createDns(removeOldDns)) {
                System.out.println("Successfully created DNS container.");

            } else {
                System.err.println("Failed to create DNS container.");
                failedSetups++;
            }
        } else {
            System.out.println("Found existing DNS container.");
        }

        // 6 - Registry certificates
        if (!creator.findRegistryCerts()) {
            System.out.println("Couldn't find required certificates for the registry. Creating them...");
            creator.generateRegistryCerts(false);

            if (creator.findRegistryCerts()) {
                System.out.println("Successfully generated the required certificates for the registry.");

            } else {
                System.err.println("Couldn't generate the required certificates for the registry.");
                failedSetups++;
            }
        } else {
            System.out.println("Found existing certificates for the registry.");
        }

        // 7 - Registry container
        if (!validator.findRegistry()) {
            System.out.println("Failed to find Registry container. Creating it...");
            if (validator.createRegistry()) {
                System.out.println("Successfully created Registry container.");

            } else {
                System.err.println("Failed to create Registry container.");
                failedSetups++;
            }
        } else {
            System.out.println("Found existing Registry container.");
        }


        System.out.println(String.format("Successfully set %s/%s services.", serviceCount - failedSetups, serviceCount));
        if (failedSetups != 0)
            System.err.println(String.format("%s services failed to start. Fix the errors before you proceed.", failedSetups));

        return failedSetups == 0;
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


    public static void main(String[] args) throws Exception {
        ServerManager serverManager = new ServerManager();

        try {
//            serverManager.onStart();
//            new BungeeProxyCreator().start();
            new LobbyPoolCreator().create();


//            serverManager.startCommandDispatcher();

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
