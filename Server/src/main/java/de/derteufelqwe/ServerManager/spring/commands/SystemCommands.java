package de.derteufelqwe.ServerManager.spring.commands;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.spring.events.CheckInfrastructureEvent;
import de.derteufelqwe.ServerManager.spring.events.ReloadConfigEvent;
import de.derteufelqwe.ServerManager.spring.events.TestEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@Log4j2
@ShellCommandGroup(value = "system")
public class SystemCommands {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private Docker docker;
    @Autowired
    private StringRedisTemplate redisTemplate;


    @ShellMethod(value = "Reloads and updates the servers config.", key = "system reload-config")
    public void reloadConfig() {
        ReloadConfigEvent reloadConfigEvent = new ReloadConfigEvent(this, ReloadConfigEvent.ReloadSource.COMMAND);
        applicationEventPublisher.publishEvent(reloadConfigEvent);

        if (reloadConfigEvent.isSuccess()) {
            log.info("Successfully reloaded server config.");

        } else {
            log.error("Config reload failed with: '{}'", reloadConfigEvent.getMessage());
        }
    }

    @ShellMethod(value = "Checks if all parts of the infrastructure are up and running or starts them.", key = "system check-infrastructure")
    public void checkInfrastructure() {
        CheckInfrastructureEvent infrastructureEvent = new CheckInfrastructureEvent(this, CheckInfrastructureEvent.ReloadSource.COMMAND);
        applicationEventPublisher.publishEvent(infrastructureEvent);

        if (infrastructureEvent.isSuccess()) {
            log.info("Infrastructure is up and running.");

        } else {
            log.error("Infrastructure setup failed. Solve the issues above to ensure full functionality.");
        }
    }


}
