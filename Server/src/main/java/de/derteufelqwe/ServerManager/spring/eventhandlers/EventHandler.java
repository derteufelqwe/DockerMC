package de.derteufelqwe.ServerManager.spring.eventhandlers;

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
import de.derteufelqwe.ServerManager.spring.events.CheckInfrastructureEvent;
import de.derteufelqwe.ServerManager.spring.events.ReloadConfigEvent;
import de.derteufelqwe.ServerManager.utils.Utils;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Log4j2
public class EventHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private Docker docker;
    @Autowired
    private Config<ServersConfig> serversConfig;
    @Autowired
    private SessionBuilder sessionBuilder;


    /**
     * Checks if the infrastructure is running and intact
     *
     * @param event
     */
    @EventListener
    public void onCheckInfrastructureEvent(CheckInfrastructureEvent event) {
        if (!this.checkAndCreateInfrastructure()) {
            event.setSuccess(false);
            event.setMessage("Failed to setup the required infrastructure. Solve the problem mentioned above.");
        }
    }

    /**
     * Reloads the minecraft server config
     *
     * @param event
     */
    @EventListener
    public void onReloadConfigEvent(ReloadConfigEvent event) {
        ServerManager.SERVERS_CONFIG.load();

        // Servers config check
        try {
            ConfigChecker checker = new ConfigChecker();
            checker.validateInfrastructureConfig(serversConfig.get());

        } catch (InvalidConfigException e) {
            event.setSuccess(false);
            event.setMessage(String.format("Invalid servers config file. Error: %s.", e.getMessage()));
            return;
        }

        // Servers setup
        if (!this.checkAndCreateMCServers()) {
            event.setSuccess(false);
            event.setMessage("Failed to setup the minecraft servers. Solve the problem mentioned above.");
        }
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
                log.info("Created overnet network successfully.");
                break;
            case RUNNING:
                log.info("Overnet network already existing.");
                break;
            case FAILED_GENERIC:
                log.error("Failed to create the Overnet network!");
                return false;

            default:
                throw new NotImplementedException("Result " + response0.getResult() + " not implemented.");
        }

        // Registry Certificates
        ServiceCreateResponse response1 = setup.createRegistryCerts();
        switch (response1.getResult()) {
            case OK:
                log.info("Created registry certificates successfully.");
                break;
            case RUNNING:
                log.info("Registry certificates already existing.");
                break;
            case FAILED_GENERIC:
                log.error("Failed to create the registry certificates! Message: {}.", response1.getAdditionalInfos());
                return false;

            default:
                throw new NotImplementedException("Result " + response1.getResult() + " not implemented.");
        }

        // Registry Container
        ServiceCreateResponse response2 = setup.createRegistryContainer();
        switch (response2.getResult()) {
            case OK:
                log.info("Created registry container successfully.");
                break;
            case RUNNING:
                log.info("Registry container already running.");
                break;
            case FAILED_GENERIC:
                log.error("Failed to create the registry container! ID: {}, Message: {}.",
                        response2.getServiceId(), response2.getAdditionalInfos());
                return false;

            default:
                throw new NotImplementedException("Result " + response2.getResult() + " not implemented.");
        }

        // Postgres Database
        ServiceCreateResponse response3 = setup.createPostgresContainer();
        switch (response3.getResult()) {
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
                        response3.getServiceId(), response3.getAdditionalInfos());
                return false;

            default:
                throw new NotImplementedException("Result " + response3.getResult() + " not implemented.");
        }

        // Logcollector service
//        ServiceCreateResponse response4 = setup.createLogcollectorService();
//        switch (response4.getResult()) {
//            case OK:
//                log.info("Created LogCollector service successfully."); break;
//            case RUNNING:
//                log.info("LogCollector service already running."); break;
//            case FAILED_GENERIC:
//                log.error("Failed to create the LogCollector service! ID: {}, Message: {}.",
//                        response4.getServiceId(), response4.getAdditionalInfos());
//                return false;
//
//            default:
//                throw new NotImplementedException("Result " + response4.getResult() + " not implemented.");
//        }

        // Redis container
        ServiceCreateResponse response5 = setup.createRedisContainer();
        switch (response5.getResult()) {
            case OK:
                log.info("Created Redis container successfully.");
                break;
            case RUNNING:
                log.info("Redis container already running.");
                break;
            case FAILED_GENERIC:
                log.error("Failed to create the Redis container! ID: {}, Message: {}.",
                        response5.getServiceId(), response5.getAdditionalInfos());
                return false;

            default:
                throw new NotImplementedException("Result " + response5.getResult() + " not implemented.");
        }

        return true;
    }

    /**
     * Creates all the servers specified in the InfrastructureConfig.yml.
     * Identifies and stops lost services.
     */
    private boolean checkAndCreateMCServers() {
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

        // BungeeCord
        ServiceUpdateResponse response1 = new BungeePoolUpdater(docker).update(false);
        switch (response1.getResult()) {
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
                        response1.getServiceId());
                return false;

            default:
                throw new NotImplementedException("Result " + response1.getResult() + " not implemented.");
        }

        // Lobby Pool
        ServiceUpdateResponse response2 = new LobbyPoolUpdater(docker, redisTemplate).update(false);
        switch (response2.getResult()) {
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
                        response2.getServiceId());
                return false;

            default:
                throw new NotImplementedException("Result " + response2.getResult() + " not implemented.");
        }


        // Other Server Pools
        for (ServerPool pool : serversConfig.getPoolServers()) {
            ServiceUpdateResponse response3 = new MinecraftPoolUpdater(docker, pool).update(false);
            switch (response3.getResult()) {
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
                            pool.getName(), response3.getServiceId());
                    return false;

                default:
                    throw new NotImplementedException("Result " + response3.getResult() + " not implemented.");
            }
        }

        return true;
    }

}
