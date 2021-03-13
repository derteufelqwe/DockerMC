package de.derteufelqwe.ServerManager.spring;

import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.ConfigChecker;
import de.derteufelqwe.ServerManager.config.ServersConfig;
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
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

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
    public boolean reloadConfig() {
        ServerManager.SERVERS_CONFIG.load();

        // Servers config check
        try {
            ConfigChecker checker = new ConfigChecker();
            checker.validateInfrastructureConfig(serversConfig.get());

        } catch (InvalidConfigException e) {
            log.error("Invalid servers config file. Error: {}.", e.getMessage());
            return false;
        }

        // Servers setup
        if (!this.checkAndCreateMCServers()) {
            log.error("Failed to setup the minecraft servers. Solve the problem mentioned above.");
            return false;
        }

        return true;
    }

    // -----  Infrastructure  -----

    /**
     * Checks if the required infrastructure exist and creates it if necessary.
     */
    public boolean checkAndCreateInfrastructure() {

        if (!this.createOvernetNetwork())
            return false;
        if (!this.createRegistryCertificates())
            return false;
        if (!this.createRegistryContainer())
            return false;
        if (!this.createPostgresContainer())
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

    public boolean createPostgresContainer() {
        InfrastructureSetup setup = new InfrastructureSetup(docker);
        ServiceCreateResponse response = setup.createPostgresContainer();

        switch (response.getResult()) {
            case OK:
                log.info("Created Postgres DB container successfully.");
                Utils.sleep(TimeUnit.SECONDS, 5);
                try {
                    sessionBuilder.init();

                } catch (ServiceException e) {
                    log.error("Failed to connect to postgres database after database setup.");
                }
                break;
            case RUNNING:
                log.info("Postgres DB container already running.");
                break;
            case FAILED_GENERIC:
                log.error("Failed to create the Postgres DB container! ID: {}, Message: {}.",
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

    // -----  Minecraft server creation / updating  -----

    /**
     * Creates all the servers specified in the InfrastructureConfig.yml.
     * Identifies and stops lost services.
     */
    public boolean checkAndCreateMCServers(boolean force) {
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
    public boolean checkAndCreateMCServers() {
        return checkAndCreateMCServers(false);
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
        ServiceUpdateResponse response = new LobbyPoolUpdater(docker, redisTemplate).update(false);

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
            ServiceUpdateResponse response = new MinecraftPoolUpdater(docker, pool).update(false);

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

}
