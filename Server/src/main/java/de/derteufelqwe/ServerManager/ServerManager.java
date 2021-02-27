package de.derteufelqwe.ServerManager;

import com.github.dockerjava.api.model.Service;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.google.common.net.HostAndPort;
import de.derteufelqwe.ServerManager.config.ConfigChecker;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.SystemConfig;
import de.derteufelqwe.ServerManager.exceptions.InvalidConfigException;
import de.derteufelqwe.ServerManager.setup.InfrastructureSetup;
import de.derteufelqwe.ServerManager.setup.LostServiceFinder;
import de.derteufelqwe.ServerManager.setup.ServiceCreateResponse;
import de.derteufelqwe.ServerManager.setup.ServiceUpdateResponse;
import de.derteufelqwe.ServerManager.setup.configUpdate.BungeePoolUpdater;
import de.derteufelqwe.ServerManager.setup.configUpdate.LobbyPoolUpdater;
import de.derteufelqwe.ServerManager.setup.configUpdate.MinecraftPoolUpdater;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.ServerManager.utils.Utils;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.ConfigOld;
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Program flow:
 * - Create required infrastructure
 * - Validate config
 * - Create servers based on config
 * <p>
 * Problems:
 * + BungeeCord servers are not synced
 * <p>
 * Problems:
 * + BungeeCord servers are not synced
 */

/**
 * Problems:
 * + BungeeCord servers are not synced
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
 * + Update services when their config changes
 * + Make config aware of changes
 * + API for BungeeCord
 * + Config checker
 * - (Copy certificates to the other servers via SSH)
 * + System to handle server logs
 * + Server History
 * - Better unhealthy state detection
 * - Clean database layout
 * (- Unify mounts in container / service template)
 * - Move from consul to redis
 */

public class ServerManager {

    private static String DOCKER_IP = "ubuntu1";
    public static ConfigOld CONFIG = new ConfigOld(new DefaultYamlConverter(), new DefaultGsonProvider(), Constants.CONFIG_PATH);

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


    // -----  Other methods  -----

    @SneakyThrows
    public void start() {
//        System.out.println(de.derteufelqwe.commons.Constants.LOGO);
//        System.out.println("> Developed by " + de.derteufelqwe.commons.Constants.AUTHOR + ".\n");
        System.out.println("Validating config...");
        try {
            new ConfigChecker().validateInfrastructureConfig();
            System.out.println("Ok.");

        } catch (InvalidConfigException e1) {
            System.err.printf("Invalid infrastructure config: %s\n", e1.getMessage());
            // ToDo: Enable config reloading
            return;
        }

        this.checkAndCreateInfrastructure();

        this.consul = Consul.builder().withHostAndPort(HostAndPort.fromParts("ubuntu1", Constants.CONSUL_PORT)).build();
        this.keyValueClient = this.consul.keyValueClient();

        this.checkAndCreateMCServers();

    }


    public void stop() throws Exception {
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

        // Overnet network
        ServiceCreateResponse response0 = setup.createOvernetNetwork();
        switch (response0.getResult()) {
            case OK:
                System.out.println("Created overnet network successfully.");
                break;
            case RUNNING:
                System.out.println("Overnet network already existing.");
                break;
            case FAILED_GENERIC:
                System.err.println("Failed to create the Overnet network!");
                break;

            default:
                throw new NotImplementedException("Result " + response0.getResult() + " not implemented.");
        }

        // Registry Certificates
        ServiceCreateResponse response1 = setup.createRegistryCerts();
        switch (response1.getResult()) {
            case OK:
                System.out.println("Created registry certificates successfully.");
                break;
            case RUNNING:
                System.out.println("Registry certificates already existing.");
                break;
            case FAILED_GENERIC:
                System.err.printf("Failed to create the registry certificates! Message: %s.",
                        response1.getAdditionalInfos());
                break;

            default:
                throw new NotImplementedException("Result " + response1.getResult() + " not implemented.");
        }

        // Registry Container
        ServiceCreateResponse response2 = setup.createRegistryContainer();
        switch (response2.getResult()) {
            case OK:
                System.out.println("Created registry container successfully.");
                break;
            case RUNNING:
                System.out.println("Registry container already running.");
                break;
            case FAILED_GENERIC:
                System.err.printf("Failed to create the registry container! ID: %s, Message: %s.%n",
                        response2.getServiceId(), response2.getAdditionalInfos());
                break;

            default:
                throw new NotImplementedException("Result " + response2.getResult() + " not implemented.");
        }

        // Consul Container
        ServiceCreateResponse response3 = setup.createConsulContainer();
        switch (response3.getResult()) {
            case OK:
                System.out.println("Created Consul service successfully.");
                break;
            case RUNNING:
                System.out.println("Consul service already running.");
                break;
            case FAILED_GENERIC:
                System.err.printf("Failed to create the Consul service! ID: %s, Message: %s.%n",
                        response3.getServiceId(), response3.getAdditionalInfos());
                break;

            default:
                throw new NotImplementedException("Result " + response3.getResult() + " not implemented.");
        }

        // Postgres Database
        ServiceCreateResponse response4 = setup.createPostgresContainer();
        switch (response4.getResult()) {
            case OK:
                System.out.println("Created Postgres DB container successfully.");
                Utils.sleep(TimeUnit.SECONDS, 5);
                break;
            case RUNNING:
                System.out.println("Postgres DB container already running.");
                break;
            case FAILED_GENERIC:
                System.err.printf("Failed to create the Postgres DB container! ID: %s, Message: %s.%n",
                        response4.getServiceId(), response4.getAdditionalInfos());
                break;

            default:
                throw new NotImplementedException("Result " + response4.getResult() + " not implemented.");
        }

        // Logcollector service
//        ServiceCreateResponse response5 = setup.createLogcollectorService();
//        switch (response5.getResult()) {
//            case OK:
//                System.out.println("Created LogCollector service successfully."); break;
//            case RUNNING:
//                System.out.println("LogCollector service already running."); break;
//            case FAILED_GENERIC:
//                System.err.printf("Failed to create the LogCollector service! ID: %s, Message: %s.%n",
//                        response5.getServiceId(), response5.getAdditionalInfos()); break;
//
//            default:
//                throw new NotImplementedException("Result " + response5.getResult() + " not implemented.");
//        }

        // Redis container
        ServiceCreateResponse response6 = setup.createRedisContainer();
        switch (response6.getResult()) {
            case OK:
                System.out.println("Created Redis container successfully.");
                break;
            case RUNNING:
                System.out.println("Redis container already running.");
                break;
            case FAILED_GENERIC:
                System.err.printf("Failed to create the Redis container! ID: %s, Message: %s.%n",
                        response6.getServiceId(), response6.getAdditionalInfos());
                break;

            default:
                throw new NotImplementedException("Result " + response6.getResult() + " not implemented.");
        }

        return true;
    }

    /**
     * Creates all the servers specified in the InfrastructureConfig.yml.
     * Identifies and stops lost services.
     */
    private void checkAndCreateMCServers() {
        LostServiceFinder cleaner = new LostServiceFinder(docker);
        List<Service> lostServices = cleaner.findLostServices();
        InfrastructureConfig infrastructureConfig = CONFIG.get(InfrastructureConfig.class);

        for (Service lostService : lostServices) {
            System.out.printf("Removing lost service %s (%s).\n", lostService.getSpec().getName(), lostService.getId());
            docker.getDocker().removeServiceCmd(lostService.getId()).exec();
            infrastructureConfig.setPoolServers(
                    infrastructureConfig.getPoolServers().stream()
                            .filter(p -> p.getName() != null && !p.getName().equals(lostService.getSpec().getLabels().get("ServerName")))
                            .collect(Collectors.toList())
            );
        }


        // BungeeCord
        ServiceUpdateResponse response1 = new BungeePoolUpdater(docker).update(false);
        switch (response1.getResult()) {
            case CREATED:
                System.out.println("BungeeCord-Pool created successfully.");
                break;
            case NOT_REQUIRED:
                System.out.println("BungeeCord-Pool already running and up-to-date.");
                break;
            case NOT_CONFIGURED:
                System.err.println("BungeeCord-Pool not configured.");
                break;
            case UPDATED:
                System.out.println("BungeeCord-Pool updating.");
                break;
            case DESTROYED:
                System.out.println("BungeeCord-Pool not configured anymore. Destroying it.");
                break;
            case FAILED_GENERIC:
                System.err.printf("Failed to create the BungeeCord-Pool. ServiceId: %s",
                        response1.getServiceId());
                break;

            default:
                throw new NotImplementedException("Result " + response1.getResult() + " not implemented.");
        }

        // Lobby Pool
        ServiceUpdateResponse response2 = new LobbyPoolUpdater(docker, keyValueClient).update(false);
        switch (response2.getResult()) {
            case CREATED:
                System.out.println("LobbyServer-Pool created successfully.");
                break;
            case NOT_REQUIRED:
                System.out.println("LobbyServer-Pool already running and up-to-date.");
                break;
            case NOT_CONFIGURED:
                System.err.println("LobbyServer-Pool not configured.");
                break;
            case UPDATED:
                System.out.println("LobbyServer-Pool updating.");
                break;
            case DESTROYED:
                System.out.println("LobbyServer-Pool not configured anymore. Destroying it.");
                break;
            case FAILED_GENERIC:
                System.err.printf("Failed to create the LobbyServer-Pool. ServiceId: %s",
                        response2.getServiceId());
                break;

            default:
                throw new NotImplementedException("Result " + response2.getResult() + " not implemented.");
        }


        // Other Server Pools
        for (ServerPool pool : CONFIG.get(InfrastructureConfig.class).getPoolServers()) {
            ServiceUpdateResponse response3 = new MinecraftPoolUpdater(docker, pool).update(false);
            switch (response3.getResult()) {
                case CREATED:
                    System.out.printf("Minecraft-Pool %s created successfully.\n", pool.getName());
                    break;
                case NOT_REQUIRED:
                    System.out.printf("Minecraft-Pool %s already running and up-to-date.\n", pool.getName());
                    break;
                case NOT_CONFIGURED:
                    System.err.printf("Minecraft-Pool %s not configured.\n", pool.getName());
                    break;
                case UPDATED:
                    System.out.printf("Minecraft-Pool %s updating.\n", pool.getName());
                    break;
                case FAILED_GENERIC:
                    System.err.printf("Failed to create the Minecraft-Pool %s. ServiceId: %s",
                            pool.getName(), response3.getServiceId());
                    break;

                default:
                    throw new NotImplementedException("Result " + response3.getResult() + " not implemented.");
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


}
