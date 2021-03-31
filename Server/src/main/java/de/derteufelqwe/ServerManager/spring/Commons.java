package de.derteufelqwe.ServerManager.spring;

import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.ConfigChecker;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.exceptions.InvalidConfigException;
import de.derteufelqwe.ServerManager.setup.*;
import de.derteufelqwe.ServerManager.setup.configUpdate.BungeePoolUpdater;
import de.derteufelqwe.ServerManager.setup.configUpdate.LobbyPoolUpdater;
import de.derteufelqwe.ServerManager.setup.configUpdate.MinecraftPoolUpdater;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.ServerManager.spring.commands.ImageCommands;
import de.derteufelqwe.ServerManager.utils.Utils;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Gathers methods, that might be used in different places
 */
@Log4j2
public class Commons {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private Docker docker;
    @Autowired
    private Config<ServersConfig> serversConfig;
    @Autowired
    private SessionBuilder sessionBuilder;


    /**
     * Reloads the minecraft servers
     * @return
     */
    public boolean reloadServerConfig() {
        ServerManager.SERVERS_CONFIG.load();

        // Servers config check
        try {
            ConfigChecker checker = new ConfigChecker();
            checker.validateInfrastructureConfig(serversConfig.get());

        } catch (InvalidConfigException e) {
            log.error("Invalid servers config file. Error: {}.", e.getMessage());
            return false;
        }

        return true;
    }

    // -----  Infrastructure setup  -----

    /**
     * Checks if the required infrastructure exist and creates it if necessary.
     */
    public boolean createFullInfrastructure() {
        if (!this.createOvernetNetwork())
            return false;
        if (!this.createRegistryCertificates())
            return false;
        if (!this.createRegistryContainer())
            return false;
        if (!this.createRedisContainer())
            return false;
        if (!this.createNodeWatcherService())
            return false;

        return true;
    }

    public boolean createOvernetNetwork() {
        InfrastructureSetup setup = new InfrastructureSetup(docker);
        ServiceCreateResponse response = setup.createOvernetNetwork();

        switch (response.getResult()) {
            case OK:
                log.info("Created overnet network successfully.");
                break;
            case RUNNING:
                log.info("Overnet network already existing.");
                break;
            case FAILED_GENERIC:
                log.error("Failed to create the Overnet network!");
                return false;

            default:
                throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
        }

        return true;
    }

    public boolean createRegistryCertificates() {
        InfrastructureSetup setup = new InfrastructureSetup(docker);
        ServiceCreateResponse response = setup.createRegistryCerts();

        switch (response.getResult()) {
            case OK:
                log.info("Created registry certificates successfully.");
                break;
            case RUNNING:
                log.info("Registry certificates already existing.");
                break;
            case FAILED_GENERIC:
                log.error("Failed to create the registry certificates! Message: {}.", response.getAdditionalInfos());
                return false;

            default:
                throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
        }

        return true;
    }

    public boolean createRegistryContainer() {
        InfrastructureSetup setup = new InfrastructureSetup(docker);
        ServiceCreateResponse response = setup.createRegistryContainer();

        switch (response.getResult()) {
            case OK:
                log.info("Created registry container successfully.");
                break;
            case RUNNING:
                log.info("Registry container already running.");
                break;
            case FAILED_GENERIC:
                log.error("Failed to create the registry container! ID: {}, Message: {}.",
                        response.getServiceId(), response.getAdditionalInfos());
                return false;

            default:
                throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
        }

        return true;
    }


    public boolean createRedisContainer() {
        InfrastructureSetup setup = new InfrastructureSetup(docker);
        ServiceCreateResponse response = setup.createRedisContainer();

        switch (response.getResult()) {
            case OK:
                log.info("Created Redis container successfully.");
                break;
            case RUNNING:
                log.info("Redis container already running.");
                break;
            case FAILED_GENERIC:
                log.error("Failed to create the Redis container! ID: {}, Message: {}.",
                        response.getServiceId(), response.getAdditionalInfos());
                return false;

            default:
                throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
        }

        return true;
    }

    public boolean createNodeWatcherService() {
        InfrastructureSetup setup = new InfrastructureSetup(docker);
        ServiceCreateResponse response = setup.createNodeWatcherService();

        switch (response.getResult()) {
            case OK:
                log.info("Created NodeWatcher service successfully."); break;
            case RUNNING:
                log.info("NodeWatcher service already running."); break;
            case FAILED_GENERIC:
                log.error("Failed to create the NodeWatcher service! ID: {}, Message: {}.",
                        response.getServiceId(), response.getAdditionalInfos());
                return false;

            default:
                throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
        }

        return true;
    }

    // -----  Infrastructure shutdown  -----

    public boolean stopInfrastructure() {
        if (!this.stopRegistryContainer())
            return false;
        if (!this.stopNodeWatcherService())
            return false;
        if (!this.stopRedisContainer())
            return false;
        if (!this.stopPostgresContainer())
            return false;

        return true;
    }

    public boolean stopRegistryContainer() {
        InfrastructureSetup setup = new InfrastructureSetup(docker);
        ServiceStopResponse response = setup.stopRegistryContainer();

        switch (response.getResult()) {
            case OK:
                log.info("Stopped registry container successfully."); break;
            case NOT_RUNNING:
                log.info("Registry container not running."); break;
            case FAILED_GENERIC:
                log.error("Failed to stop the registry container! ID: {}, Message: {}.",
                        response.getServiceId(), response.getAdditionalInfos());
                return false;

            default:
                throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
        }

        return true;
    }

    public boolean stopPostgresContainer() {
        InfrastructureSetup setup = new InfrastructureSetup(docker);
        ServiceStopResponse response = setup.stopPostgresContainer();

        switch (response.getResult()) {
            case OK:
                log.info("Stopped postres container successfully."); break;
            case NOT_RUNNING:
                log.info("Postgres container not running."); break;
            case FAILED_GENERIC:
                log.error("Failed to stop the postgres container! ID: {}, Message: {}.",
                        response.getServiceId(), response.getAdditionalInfos());
                return false;

            default:
                throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
        }

        return true;
    }

    public boolean stopNodeWatcherService() {
        InfrastructureSetup setup = new InfrastructureSetup(docker);
        ServiceStopResponse response = setup.stopNodeWatcherService();

        switch (response.getResult()) {
            case OK:
                log.info("Stopped NodeWatcher service successfully."); break;
            case NOT_RUNNING:
                log.info("NodeWatcher service not running."); break;
            case FAILED_GENERIC:
                log.error("Failed to stop the NodeWatcher service! ID: {}, Message: {}.",
                        response.getServiceId(), response.getAdditionalInfos());
                return false;

            default:
                throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
        }

        return true;
    }

    public boolean stopRedisContainer() {
        InfrastructureSetup setup = new InfrastructureSetup(docker);
        ServiceStopResponse response = setup.stopRedisContainer();

        switch (response.getResult()) {
            case OK:
                log.info("Stopped redis container successfully."); break;
            case NOT_RUNNING:
                log.info("Redis container not running."); break;
            case FAILED_GENERIC:
                log.error("Failed to stop the redis container! ID: {}, Message: {}.",
                        response.getServiceId(), response.getAdditionalInfos());
                return false;

            default:
                throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
        }

        return true;
    }

    // -----  Minecraft server setup  -----

    /**
     * Creates all the servers specified in the InfrastructureConfig.yml.
     * Identifies and stops lost services.
     */
    public boolean createAllMCServers(boolean force) {
        LostServiceFinder cleaner = new LostServiceFinder(docker);
        List<Service> lostServices = cleaner.findLostServices();
        ServersConfig serversConfig = this.serversConfig.get();

        for (Service lostService : lostServices) {
            log.warn("Removing lost service {} ({}).", lostService.getSpec().getName(), lostService.getId());

            docker.getDocker().removeServiceCmd(lostService.getId()).exec();
            String serviceName = lostService.getSpec().getLabels().get("ServerName");
            serversConfig.getPoolServers().removeIf(
                    p -> p.getName().equals(serviceName)
            );
        }


        if (!createBungeeServer(force))
            return false;

        if (!createLobbyServer(force))
            return false;

        if (!createPoolServers(force))
            return false;

        return true;
    }

    /**
     * Default server creation method
     * @return
     */
    public boolean createAllMCServers() {
        return createAllMCServers(false);
    }

    /**
     * Creates or updates the BungeeCord service
     * @param force
     * @return
     */
    public boolean createBungeeServer(boolean force) {
        ServiceUpdateResponse response = new BungeePoolUpdater(docker).update(force);

        switch (response.getResult()) {
            case CREATED:
                log.info("BungeeCord-Pool created successfully.");
                break;
            case NOT_REQUIRED:
                log.info("BungeeCord-Pool already running and up-to-date.");
                break;
            case NOT_CONFIGURED:
                log.warn("BungeeCord-Pool not configured.");
                break;
            case UPDATED:
                log.info("BungeeCord-Pool updating.");
                break;
            case DESTROYED:
                log.info("BungeeCord-Pool not configured anymore. Destroying it.");
                break;
            case FAILED_GENERIC:
                log.error("Failed to create the BungeeCord-Pool. ServiceId: {}",
                        response.getServiceId());
                return false;

            default:
                throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
        }

        return true;
    }

    /**
     * Creates or updates the LobbyServer service
     * @param force
     * @return
     */
    public boolean createLobbyServer(boolean force) {
        ServiceUpdateResponse response = new LobbyPoolUpdater(docker, redisTemplate).update(force);

        switch (response.getResult()) {
            case CREATED:
                log.info("LobbyServer-Pool created successfully.");
                break;
            case NOT_REQUIRED:
                log.info("LobbyServer-Pool already running and up-to-date.");
                break;
            case NOT_CONFIGURED:
                log.warn("LobbyServer-Pool not configured.");
                break;
            case UPDATED:
                log.info("LobbyServer-Pool updating.");
                break;
            case DESTROYED:
                log.info("LobbyServer-Pool not configured anymore. Destroying it.");
                break;
            case FAILED_GENERIC:
                log.error("Failed to create the LobbyServer-Pool. ServiceId: {}",
                        response.getServiceId());
                return false;

            default:
                throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
        }

        return true;
    }

    public boolean createPoolServers(boolean force) {
        ServersConfig serversConfig = this.serversConfig.get();

        for (ServerPool pool : serversConfig.getPoolServers()) {
            ServiceUpdateResponse response = new MinecraftPoolUpdater(docker, pool).update(force);

            switch (response.getResult()) {
                case CREATED:
                    log.info("Minecraft-Pool {} created successfully.", pool.getName());
                    break;
                case NOT_REQUIRED:
                    log.info("Minecraft-Pool {} already running and up-to-date.", pool.getName());
                    break;
                case NOT_CONFIGURED:
                    log.error("Minecraft-Pool {} not configured.", pool.getName());
                    break;
                case UPDATED:
                    log.info("Minecraft-Pool {} updating.", pool.getName());
                    break;
                case FAILED_GENERIC:
                    log.error("Failed to create the Minecraft-Pool {}. ServiceId: {}",
                            pool.getName(), response.getServiceId());
                    return false;

                default:
                    throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
            }
        }

        return true;
    }

    // -----  Minecraft server shutdown  -----

    public boolean stopAllMCServers() {
        List<Service> services = docker.getDocker().listServicesCmd()
                .withLabelFilter(Constants.DOCKER_IDENTIFIER_MAP)
                .exec();
        log.info("Found {} active services.", services.size());

        for (Service service : services) {
            String name = service.getSpec().getName();
            String type = service.getSpec().getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY);

            if (!(type.equals(Constants.ContainerType.BUNGEE_POOL.name()) || type.equals(Constants.ContainerType.MINECRAFT_POOL.name()))) {
                continue;
            }

            docker.getDocker().removeServiceCmd(service.getId()).exec();
            log.info("Removed {} service {} ({}).", type, name, service.getId());
        }

        return true;
    }

    private boolean stopGenericServer(String serviceName, Constants.ContainerType type, String logName) {
        List<Service> services = docker.getDocker().listServicesCmd()
                .withLabelFilter(Constants.DOCKER_IDENTIFIER_MAP)
                .withLabelFilter(Collections.singletonMap(Constants.CONTAINER_IDENTIFIER_KEY, type.name()))
                .withNameFilter(Collections.singletonList(serviceName))
                .exec();
        log.info("Found {} active {} services.", services.size(), logName);

        for (Service service : services) {
            String name = service.getSpec().getName();

            docker.getDocker().removeServiceCmd(service.getId()).exec();
            log.info("Removed {} service {} ({}).", logName, name, service.getId());
        }

        return true;
    }

    public boolean stopBungeeServer() {
        return stopGenericServer(null, Constants.ContainerType.BUNGEE_POOL, "Bungee");
    }

    public boolean stopLobbyServer() {
        ServerPool pool = serversConfig.get().getLobbyPool();
        if (pool == null) {
            log.info("LobbyPool not configured.");
            return false;
        }

        return stopGenericServer(pool.getName(), Constants.ContainerType.MINECRAFT_POOL, "LobbyServer");
    }

    public boolean stopPoolServer(String serverName) {
        ServerPool pool = null;
        for (ServerPool serverPool : serversConfig.get().getPoolServers()) {
            if (serverPool.getName().equals(serverName)) {
                pool = serverPool;
                break;
            }
        }

        if (pool == null) {
            log.warn("Pool server {} is not present.", serverName);
            return false;
        }

        return stopGenericServer(pool.getName(), Constants.ContainerType.MINECRAFT_POOL, pool.getName());
    }

}
