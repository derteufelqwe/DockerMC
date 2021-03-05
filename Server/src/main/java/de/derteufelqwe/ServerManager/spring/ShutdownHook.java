package de.derteufelqwe.ServerManager.spring;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

@Component
@Log4j2
public class ShutdownHook {

    @Autowired
    private Docker docker;

    /**
     * Called when the system shuts down.
     */
    @PreDestroy
    public void onStop() {
        try {
            log.info("Closing connection to docker API.");
            docker.getDocker().close();

        } catch (IOException e) {
            log.error("Failed to close docker connection. Error: {}.", e.getMessage());
        }
    }


}
