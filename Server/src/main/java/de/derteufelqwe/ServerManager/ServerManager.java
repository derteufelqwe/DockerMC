package de.derteufelqwe.ServerManager;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.commands.*;
import de.derteufelqwe.ServerManager.config.Config;
import de.derteufelqwe.ServerManager.config.configs.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.configs.MainConfig;
import de.derteufelqwe.ServerManager.config.configs.RunningConfig;
import de.derteufelqwe.ServerManager.config.configs.objects.BungeeProxy;
import de.derteufelqwe.ServerManager.config.configs.objects.PersistentServerPool;
import de.derteufelqwe.ServerManager.config.configs.objects.ServerPool;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.BaseContainerCreator;
import de.derteufelqwe.ServerManager.setup.CertificateCreator;
import de.derteufelqwe.ServerManager.setup.MCServerCreator;
import de.derteufelqwe.ServerManager.setup.servers.BungeeProxyCreator;
import de.derteufelqwe.ServerManager.setup.servers.MCServerDestroyer;
import de.derteufelqwe.ServerManager.setup.servers.ServerPoolCreator;
import de.derteufelqwe.ServerManager.setup.servers.responses.BungeeResponse;
import de.derteufelqwe.ServerManager.setup.servers.responses.PoolResponse;
import de.derteufelqwe.commons.Constants;
import lombok.Getter;
import org.bouncycastle.pqc.jcajce.provider.McEliece;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ServerManager {

    static {
        Config.registerConfig(MainConfig.class, "", Constants.Configs.MAIN.filename());
        Config.registerConfig(RunningConfig.class, "", Constants.Configs.RUNNING.filename());
        Config.registerConfig(InfrastructureConfig.class, "", Constants.Configs.INFRASTRUCTURE.filename());
        Config.loadAll();
    }

    @Getter
    private static Docker docker = new Docker();


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
        BaseContainerCreator creator = new BaseContainerCreator();
        CertificateCreator certCreator = new CertificateCreator();
        boolean removeOldDns = true;
        boolean createProxyCerts = false;
        int serviceCount = 8;
        int failedSetups = 0;

        System.out.println("Checking and setting up infrastructure...");

        // 1 - Overnet network
        if (!creator.findNetworkOvernet()) {
            System.out.println(String.format("Failed to find network %s. Creating it...", Constants.NETW_OVERNET_NAME));
            if (creator.createNetworkOvernet()) {
                System.out.println(String.format("Successfully created network %s.", Constants.NETW_OVERNET_NAME));

            } else {
                System.err.println(String.format("Failed to create network %s.", Constants.NETW_OVERNET_NAME));
                failedSetups++;
            }
        } else {
            System.out.println(String.format("Found existing network %s.", Constants.NETW_OVERNET_NAME));
        }

        // 2 - API_net network
        if (!creator.findNetworkApiNet()) {
            System.out.println(String.format("Failed to find network %s. Creating it...", Constants.NETW_API_NAME));
            if (creator.createNetworkApiNet()) {
                System.out.println(String.format("Successfully created network %s.", Constants.NETW_API_NAME));

            } else {
                System.err.println(String.format("Failed to create network %s.", Constants.NETW_API_NAME));
                failedSetups++;
            }
        } else {
            System.out.println(String.format("Found existing network %s.", Constants.NETW_API_NAME));
        }

        // 3 - API-proxy certificates
        if (!certCreator.findAPIProxyCerts()) {
            System.out.println("Couldn't find required certificates for the API-proxy. Generating them...");
            String containerID = certCreator.generateAPIProxyCerts(false);

            if (certCreator.findAPIProxyCerts()) {
                System.out.println("Successfully generated the certificates for the API-proxy.");

            } else {
                System.err.println("Failed to create the API-proxy certificates.");
                System.out.println(docker.getContainerLog(containerID));
                failedSetups++;
            }
        } else {
            System.out.println("Found existing API-proxy certificates.");
        }

        // 4 - API-proxy container
        if (!creator.findAPIProxy()) {
            System.out.println("Failed to find API-proxy container. Creating it...");
            if (creator.createAPIProxy(createProxyCerts)) {
                System.out.println("Successfully created API-proxy container.");

            } else {
                System.err.println("Failed to create API-proxy container.");
                failedSetups++;
            }
        } else {
            System.out.println("Found existing API-proxy container.");
        }

        // 5 - DNS container
        if (!creator.findDns()) {
            System.out.println("Failed to find DNS container. Creating it...");
            if (creator.createDns(removeOldDns)) {
                System.out.println("Successfully created DNS container.");

            } else {
                System.err.println("Failed to create DNS container.");
                failedSetups++;
            }
        } else {
            System.out.println("Found existing DNS container.");
        }

        // 6 - Registry certificates
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

        // 7 - Registry container
        if (!creator.findRegistry()) {
            System.out.println("Failed to find Registry container. Creating it...");
            if (creator.createRegistry()) {
                System.out.println("Successfully created Registry container.");

            } else {
                System.err.println("Failed to create Registry container.");
                failedSetups++;
            }
        } else {
            System.out.println("Found existing Registry container.");
        }

        // 8 - Config webserver
        if (!creator.findConfigWebserver()) {
            System.out.println("Failed to find Config webserver container. Creating it...");
            if (creator.createConfigWebserver()) {
                System.out.println("Successfully created config webserver container.");

            } else {
                System.err.println("Failed to create config webserver container.");
                failedSetups++;
            }
        } else {
            System.out.println("Found existing config webserver container.");
        }


        System.out.println(String.format("Successfully set %s/%s services.", serviceCount - failedSetups, serviceCount));
        if (failedSetups != 0)
            System.err.println(String.format("%s services failed to start. Fix the errors before you proceed.", failedSetups));

        return failedSetups == 0;
    }

    /**
     * ToDo: Save logs when logger is added
     * Creates all the servers specified in the InfrastructureConfig.yml.
     * @return Successfully created all server or not
     */
    private boolean checkAndCreateMCServers() {
        MCServerCreator creator = new MCServerCreator();
        int successfulStarts = 0;
        int failedStarts = 0;


        // 1 - Bungee Proxy
        List<BungeeResponse> proxyResponse = creator.createBungeeProxy();
        for (BungeeResponse response : proxyResponse) {
            if (response.getConfig() == null) {
                failedStarts++;

            } else if (!response.successful()) {
                BungeeProxy cfg = (BungeeProxy) response.getConfig();
                System.err.println(String.format("Failed to create service %s for proxy server %s with %s.",
                        response.getObjectID(), cfg.getName(), response.getCause()));
//                System.out.println("Log: " + response.getLogs());
                System.out.println("Failed containers: " + response.getFailedContainers().stream().map(Container::getId).collect(Collectors.joining(", ")));

                failedStarts++;

            } else {
                try {
                    // Wait so the bungeecord proxy can actually start
                    TimeUnit.SECONDS.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                successfulStarts++;
            }
        }


        // 2 - Lobby Pool
        List<PoolResponse> lobbyResponse = creator.createLobbyServers();
        for (PoolResponse response : lobbyResponse) {
            if (response.getConfig() == null) {
                failedStarts++;

            } else if (!response.successful()) {
                ServerPool cfg = (ServerPool) response.getConfig();
                System.err.println(String.format("Failed to create service %s for lobby server %s with %s.",
                        response.getObjectID(), cfg.getName(), response.getCause()));
//                System.out.println("Log: " + response.getLogs());
                System.out.println("Failed containers: " + response.getFailedContainers().stream().map(Container::getId).collect(Collectors.joining(", ")));

                failedStarts++;

            } else {
                successfulStarts++;
            }
        }


        // 3 - Pool servers
        List<PoolResponse> poolResponses = creator.createPoolServers();
        for (PoolResponse response : lobbyResponse) {
            if (response.getConfig() == null) {
                failedStarts++;

            } else if (!response.successful()) {
                ServerPool cfg = (ServerPool) response.getConfig();
                System.err.println(String.format("Failed to create service %s for pool server %s with %s.",
                        response.getObjectID(), cfg.getName(), response.getCause()));
//                System.out.println("Log: " + response.getLogs());
                System.out.println("Failed containers: " + response.getFailedContainers().stream().map(Container::getId).collect(Collectors.joining(", ")));

                failedStarts++;

            } else {
                successfulStarts++;
            }
        }


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
        MCServerDestroyer destroyer = new MCServerDestroyer();
        System.out.println(destroyer.destroy("Lobby"));
        System.out.println(destroyer.destroy("Build"));
        System.out.println(destroyer.destroy("Creative"));
        System.out.println(destroyer.destroy("Test"));
    }


    public static void main(String[] args) throws Exception {
        ServerManager serverManager = new ServerManager();

        try {
//            serverManager.onStart();
//            serverManager.checkAndCreateMCServers();


            Service service = docker.getDocker().inspectServiceCmd("7a6g4c07bh8d").exec();
            List<Container> container = docker.getDocker().listContainersCmd().exec();
            Network network = docker.getDocker().inspectNetworkCmd().withNetworkId("omf7losexi8v").exec();

            System.out.println("s");

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
