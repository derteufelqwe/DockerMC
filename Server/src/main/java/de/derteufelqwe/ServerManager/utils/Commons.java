package de.derteufelqwe.ServerManager.utils;

import com.github.dockerjava.api.model.Service;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.config.ConfigChecker;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.exceptions.InvalidConfigException;
import de.derteufelqwe.ServerManager.setup.InfrastructureSetup;
import de.derteufelqwe.ServerManager.setup.LostServiceFinder;
import de.derteufelqwe.ServerManager.setup.ServiceCreateResponse;
import de.derteufelqwe.ServerManager.setup.ServiceStopResponse;
import de.derteufelqwe.ServerManager.setup.configUpdate.BungeePoolCreator;
import de.derteufelqwe.ServerManager.setup.configUpdate.LobbyPoolCreator;
import de.derteufelqwe.ServerManager.setup.configUpdate.MinecraftPoolCreator;
import de.derteufelqwe.ServerManager.setup.configUpdate.PersistentMinecraftPoolCreator;
import de.derteufelqwe.ServerManager.setup.servers.PersistentServerPool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.NotImplementedException;
import redis.clients.jedis.JedisPool;

import java.util.Collections;
import java.util.List;

/**
 * Gathers methods, that might be used in different places
 */
@Log4j2
public class Commons {

    @Inject private Config<MainConfig> mainConfig;
    @Inject @NewConfig private Config<ServersConfig> serversConfig;
    @Inject @OldConfig private Config<ServersConfig> serversConfigOld;
    @Inject private Docker docker;
    @Inject private JedisPool jedisPool;
    @Inject private InfrastructureSetup setup;

    public Commons() {

    }


    /**
     * Reloads the minecraft servers
     *
     * @return
     */
    public boolean reloadServerConfig() {
        ServersConfig backup = serversConfig.get();
        serversConfig.load();

        // Servers config check
        try {
            ConfigChecker checker = new ConfigChecker();
            checker.validateInfrastructureConfig(serversConfig.get());

        } catch (InvalidConfigException e) {
            log.error("Invalid servers config file. Config didn't update.", e);
            serversConfig.set(backup);
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
        ServiceCreateResponse response = setup.createNodeWatcherService();

        switch (response.getResult()) {
            case OK:
                log.info("Created NodeWatcher service successfully.");
                break;
            case RUNNING:
                log.info("NodeWatcher service already running.");
                break;
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

        return true;
    }

    public boolean stopRegistryContainer() {
        ServiceStopResponse response = setup.stopRegistryContainer();

        switch (response.getResult()) {
            case OK:
                log.info("Stopped registry container successfully.");
                break;
            case NOT_RUNNING:
                log.info("Registry container not running.");
                break;
            case FAILED_GENERIC:
                log.error("Failed to stop the registry container! ID: {}, Message: {}.",
                        response.getServiceId(), response.getAdditionalInfos());
                return false;

            default:
                throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
        }

        return true;
    }

    public boolean stopNodeWatcherService() {
        ServiceStopResponse response = setup.stopNodeWatcherService();

        switch (response.getResult()) {
            case OK:
                log.info("Stopped NodeWatcher service successfully.");
                break;
            case NOT_RUNNING:
                log.info("NodeWatcher service not running.");
                break;
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
        ServiceStopResponse response = setup.stopRedisContainer();

        switch (response.getResult()) {
            case OK:
                log.info("Stopped redis container successfully.");
                break;
            case NOT_RUNNING:
                log.info("Redis container not running.");
                break;
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

    public void removeLostServices() {
        LostServiceFinder cleaner = new LostServiceFinder(docker, serversConfig);
        List<Service> lostServices = cleaner.findLostServices();
        ServersConfig serversConfig = this.serversConfig.get();
        ServersConfig serversConfigOld = this.serversConfigOld.get();

        for (Service lostService : lostServices) {
            log.warn("Removing lost service {} ({}).", lostService.getSpec().getName(), lostService.getId());

            docker.getDocker().removeServiceCmd(lostService.getId()).exec();
            String serviceName = lostService.getSpec().getLabels().get("ServerName");

            serversConfig.getPoolServers().removeIf(p -> p.getName().equals(serviceName));
            serversConfig.getPersistentServerPool().removeIf(p -> p.getName().equals(serviceName));
            serversConfigOld.getPoolServers().removeIf(p -> p.getName().equals(serviceName));
            serversConfigOld.getPersistentServerPool().removeIf(p -> p.getName().equals(serviceName));
        }

        this.serversConfig.get();
        this.serversConfigOld.save();
    }

    /**
     * Creates all the servers specified in the InfrastructureConfig.yml.
     * Identifies and stops lost services.
     */
    public boolean createAllMCServers(boolean force) {
        this.removeLostServices();

        if (!createBungeeServer(force))
            return false;

        if (!createLobbyServer(force))
            return false;

        if (!createAllPoolServers(force))
            return false;

        if (!createAllPersistentPoolServers(force))
            return false;

        return true;
    }


    /**
     * Creates or updates the BungeeCord service
     *
     * @param force
     * @return
     */
    public boolean createBungeeServer(boolean force) {
        ServiceCreateResponse response = new BungeePoolCreator(docker, mainConfig, serversConfig, serversConfigOld).createOrUpdate(force);

        switch (response.getResult()) {
            case CREATED:
                log.info("BungeeCord-Pool created successfully.");
                break;
            case RUNNING:
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
     *
     * @param force
     * @return
     */
    public boolean createLobbyServer(boolean force) {
        ServiceCreateResponse response = new LobbyPoolCreator(docker, mainConfig, serversConfig, serversConfigOld, jedisPool).createOrUpdate(force);

        switch (response.getResult()) {
            case CREATED:
                log.info("LobbyServer-Pool created successfully.");
                break;
            case RUNNING:
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

    public boolean createAllPoolServers(boolean force) {
        ServersConfig serversConfig = this.serversConfig.get();

        for (ServerPool pool : serversConfig.getPoolServers()) {
            this.createPoolServer(pool, force);
        }

        return true;
    }

    public boolean createPoolServer(String serverName, boolean force) {
        ServerPool serverPool = null;
        for (ServerPool pool : serversConfig.get().getPoolServers()) {
            if (pool.getName().equals(serverName)) {
                serverPool = pool;
                break;
            }
        }

        if (serverPool == null) {
            log.warn("Pool server {} not found.", serverName);
            return false;
        }

        return this.createPoolServer(serverPool, force);
    }

    public boolean createPoolServer(ServerPool pool, boolean force) {
        ServiceCreateResponse response = new MinecraftPoolCreator(docker, pool, mainConfig, serversConfig, serversConfigOld).createOrUpdate(force);

        switch (response.getResult()) {
            case CREATED:
                log.info("Minecraft-Pool {} created successfully.", pool.getName());
                break;
            case RUNNING:
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

        return true;
    }

    public boolean createPersistentPoolServer(String serverName, boolean force) {
        PersistentServerPool serverPool = null;
        for (PersistentServerPool pool : serversConfig.get().getPersistentServerPool()) {
            if (pool.getName().equals(serverName)) {
                serverPool = pool;
                break;
            }
        }

        if (serverPool == null) {
            log.warn("Persistent pool server {} not found.", serverName);
            return false;
        }

        return this.createPersistentPoolServer(serverPool, force);
    }

    public boolean createPersistentPoolServer(PersistentServerPool pool, boolean force) {
        ServiceCreateResponse response = new PersistentMinecraftPoolCreator(docker, pool, mainConfig, serversConfig, serversConfigOld).createOrUpdate(force);

        switch (response.getResult()) {
            case CREATED:
                log.info("Persistent minecraft-Pool {} created successfully.", pool.getName());
                break;
            case RUNNING:
                log.info("Persistent minecraft-Pool {} already running and up-to-date.", pool.getName());
                break;
            case NOT_CONFIGURED:
                log.error("Persistent minecraft-Pool {} not configured.", pool.getName());
                break;
            case UPDATED:
                log.info("Persistent minecraft-Pool {} updating.", pool.getName());
                break;
            case FAILED_GENERIC:
                log.error("Failed to create the persistent minecraft-Pool {}. ServiceId: {}",
                        pool.getName(), response.getServiceId());
                return false;

            default:
                throw new NotImplementedException("Result " + response.getResult() + " not implemented.");
        }

        return true;
    }

    public boolean createAllPersistentPoolServers(boolean force) {
        ServersConfig serversConfig = this.serversConfig.get();

        for (PersistentServerPool pool : serversConfig.getPersistentServerPool()) {
            this.createPersistentPoolServer(pool, force);
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
