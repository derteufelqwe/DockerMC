package de.derteufelqwe.ServerManager;

import de.derteufelqwe.ServerManager.commands.*;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.RunningConfig;
import de.derteufelqwe.ServerManager.config.backend.Config;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.infrastructure.CertificateCreator;
import de.derteufelqwe.ServerManager.setup.infrastructure.ConsulService;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;
import lombok.Getter;
import picocli.CommandLine;

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
//            serverManager.onStart();
//            serverManager.checkAndCreateMCServers();

//            InfrastructureConfig config = Config.get(InfrastructureConfig.class);
//
//            ServerPool lobby = config.getLobbyPool();
//
//            if (lobby != null) {
//                ServerBase.ValidationResponse validationResponse = lobby.valid();
//                System.out.println(validationResponse);
//
//                if (validationResponse.isValid()) {
//                    lobby.init(ServerManager.getDocker());
//                    ServerBase.FindResponse lobbyResponse = lobby.find();
//                    System.out.println(lobbyResponse);
//
//                    if (lobbyResponse.isFound()) {
//                        lobby.destroy();
//                        TimeUnit.SECONDS.sleep(1);
//                        System.out.println(lobby.create());
//
//                    } else {
//                        System.out.println(lobby.create());
//                    }
//
//                } else {
//                    System.out.println(validationResponse.getReason());
//                }
//            }

            ServerPool serverPool = new ServerPool("testmc", "1G", "2", "TestMC", 2, null, 2);
            serverPool.init(docker);
            System.out.println(serverPool.create());

//            ConsulService consulService = new ConsulService(docker);
//            System.out.println(consulService.create());


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
