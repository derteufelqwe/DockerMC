package de.derteufelqwe.ServerManager;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.google.common.net.HostAndPort;
import de.derteufelqwe.ServerManager.commands.*;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.RunningConfig;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.DockerObjTemplate;
import de.derteufelqwe.ServerManager.setup.infrastructure.ConsulService;
import de.derteufelqwe.ServerManager.setup.infrastructure.NginxService;
import de.derteufelqwe.ServerManager.setup.infrastructure.RegistryCertificates;
import de.derteufelqwe.ServerManager.setup.infrastructure.RegistryContainer;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import lombok.Getter;
import picocli.CommandLine;

import java.util.List;
import java.util.Scanner;

public class ServerManager {

    private static String DOCKER_IP = "ubuntu1";
    public static Config CONFIG = new Config(new DefaultYamlConverter(), new DefaultGsonProvider());

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
        int serviceCount = 3;
        int failedSetups = 0;

        System.out.println("Checking and setting up infrastructure...");

        // 1. Registry certificates
        RegistryCertificates registryCertificates = new RegistryCertificates(docker);
        if (!registryCertificates.find().isFound()) {
            System.out.println("Couldn't find required certificates for the registry. Creating them...");
            registryCertificates.create();

            if (registryCertificates.find().isFound()) {
                System.out.println("Successfully generated the required certificates for the registry.");

            } else {
                System.err.println("Couldn't generate the required certificates for the registry.");
                failedSetups++;
            }
        } else {
            System.out.println("Found existing certificates for the registry.");
        }

        // 2. Registry container
        RegistryContainer registryContainer = new RegistryContainer();
        registryContainer.init(docker);
        if (!registryContainer.find().isFound()) {
            System.out.println("Couldn't find registry container. Creating it...");

            DockerObjTemplate.CreateResponse createResponse = registryContainer.create();
            if (createResponse.isCreated()) {
                System.out.println("Successfully created registry container.");

            } else {
                System.err.println("Failed to create registry container.");
                System.err.println(createResponse.getMessage());
                failedSetups++;
            }

        } else {
            System.out.println("Found existing registry container.");
        }

        // 3. Consul service
        ConsulService consulService = new ConsulService();
        consulService.init(docker);
        if (!consulService.find().isFound()) {
            System.out.println("Couldn't find consul service. Creating it...");

            if (consulService.create().isCreated()) {
                System.out.println("Successfully created consul service.");

            } else {
                System.err.println("Failed to create consul service.");
                failedSetups++;
            }

        } else {
            System.out.println("Found existing consul service.");
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
        InfrastructureConfig cfg = CONFIG.get(InfrastructureConfig.class);
        int serviceCount = 4;
        int failedStarts = 0;

        // 1. NginxService
        NginxService nginxService = cfg.getNginxService();
        if (nginxService == null) {
            System.err.println("No nginx service configured.");
            failedStarts++;

        } else {
            nginxService.init(docker);

            if (nginxService.find().isFound()) {
                System.out.println("Found existing nginx service.");

            } else {
                System.out.println("Couldn't find existing nginx service. Creating it...");

                if (nginxService.create().isCreated()) {
                    System.out.println("Successfully created nginx service.");

                } else {
                    System.err.println("Failed to create nginx service.");
                    failedStarts++;
                }
            }
        }

        // 2. BungeePool
        BungeePool bungeePool = cfg.getBungeePool();
        if (bungeePool == null) {
            System.err.println("No bungee service configured.");
            failedStarts++;

        } else {
            bungeePool.init(docker);

            if (bungeePool.find().isFound()) {
                System.out.println("Found existing bungee service.");

            } else {
                System.out.println("Couldn't find existing bungee service. Creating it...");

                if (bungeePool.create().isCreated()) {
                    System.out.println("Successfully created bungee service.");

                } else {
                    System.err.println("Failed to create bungee service.");
                    failedStarts++;
                }
            }
        }

        // 3. Looby
        ServerPool lobbyPool = cfg.getLobbyPool();
        if (lobbyPool == null) {
            System.err.println("No lobby service configured.");
            failedStarts++;

        } else {
            lobbyPool.init(docker);

            if (lobbyPool.find().isFound()) {
                System.out.println("Found existing lobby service.");

            } else {
                System.out.println("Couldn't find existing lobby service. Creating it...");

                if (lobbyPool.create().isCreated()) {
                    System.out.println("Successfully created lobby service.");
                    keyValueClient.putValue("system/lobbyServerName", lobbyPool.getName());

                } else {
                    System.err.println("Failed to create lobby service.");
                    failedStarts++;
                }
            }
        }

        // 4. Pool Server
        for (ServerPool pool : cfg.getPoolServers()) {
            pool.init(docker);

            if (pool.find().isFound()) {
                System.out.println(String.format("Found existing server pool service %s.", pool.getName()));

            } else {
                System.out.println(String.format("Couldn't find existing pool server service %s. Creating it...", pool.getName()));

                if (pool.create().isCreated()) {
                    System.out.println(String.format("Successfully created pool server service %s.", pool.getName()));
                    serviceCount++;

                } else {
                    System.err.println(String.format("Failed to create pool server service %s.", pool.getName()));
                    failedStarts++;
                }
            }

        }

        System.out.println(String.format("Successfully started %s / %s services.", serviceCount, serviceCount - failedStarts));
        return failedStarts == 0 && serviceCount > 0;
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

//            serverManager.checkAndCreateMCServers();

//            NginxService nginxService = new NginxService("NginxProxy", "mcproxy", "512M", "1", 1,
//                    new ServiceConstraints(1), 25577);
//            nginxService.init(docker);
//            if (nginxService.find().isFound()) {
//                nginxService.destroy();
//            }
//            nginxService.create();

//            BungeePool bungeePool = new BungeePool("BungeeCord", "waterfall", "512M", "1", 1, new ServiceConstraints(1));
//            bungeePool.init(docker);
//            if (bungeePool.find().isFound()) {
//                bungeePool.destroy();
//            }
//            bungeePool.create();

//            ServerPool lobbyPool = new ServerPool("Lobby", "testmc", "512M", "1", 2, null, 5);
//            lobbyPool.init(docker);
//            System.out.println(lobbyPool.create());

//            ServiceConstraints constraints = new ServiceConstraints(Collections.singletonList("xtjj96fihmrwq0rqo1c89nna8"), null, null, 0);
//            ServerPool serverPool = new ServerPool("Minigame-1", "testmc", "512M", "1", 2, constraints, 2);
//            serverPool.init(docker);
//            System.out.println(serverPool.create());

//            ServerPool serverPool2 = new ServerPool("Minigame-2", "testmc", "512M", "1", 2, null, 2);
//            serverPool2.init(docker);
//            System.out.println(serverPool2.create());


        } finally {
            serverManager.onExit();
        }

    }

}
