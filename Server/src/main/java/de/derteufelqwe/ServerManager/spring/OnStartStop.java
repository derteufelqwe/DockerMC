package de.derteufelqwe.ServerManager.spring;

import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.config.ConfigChecker;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.config.MainConfig;
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
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.NotImplementedException;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Log4j2
public class OnStartStop {

    @Autowired
    private Environment environment;
    @Autowired
    private ApplicationContext appContext;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private Docker docker;
    @Autowired
    private Config<MainConfig> mainConfig;
    @Autowired
    private Config<ServersConfig> serversConfig;


    @PostConstruct
    public void onStart() {
        // Infrastructure setup
        log.info("Setting infrastructure up...");
        CheckInfrastructureEvent infrastructureEvent = new CheckInfrastructureEvent(this, CheckInfrastructureEvent.ReloadSource.APPLICATION_START);
        appContext.publishEvent(infrastructureEvent);

        if (!infrastructureEvent.isSuccess()) {
            log.error("Infrastructure setup failed with: {}", infrastructureEvent.getMessage());
            SpringApplication.exit(appContext, () -> 100);
        }

        // Servers config check
        ReloadConfigEvent reloadConfigEvent = new ReloadConfigEvent(this, ReloadConfigEvent.ReloadSource.APPLICATION_START);
        appContext.publishEvent(reloadConfigEvent);

        if (!reloadConfigEvent.isSuccess()) {
            log.fatal("Minecraft server setup failed with: '{}'", reloadConfigEvent.getMessage());
            SpringApplication.exit(appContext, () -> 102);
        }
    }

    @PreDestroy
    public void onStop() {
        try {
            docker.getDocker().close();

        } catch (IOException e) {
            log.error("Failed to close docker connection. Error: {}.", e.getMessage());
        }
    }





}
