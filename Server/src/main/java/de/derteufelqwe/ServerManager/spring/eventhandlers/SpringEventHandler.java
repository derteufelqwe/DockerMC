package de.derteufelqwe.ServerManager.spring.eventhandlers;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.callbacks.ImagePullCallback;
import de.derteufelqwe.ServerManager.spring.Commons;
import de.derteufelqwe.ServerManager.spring.events.CheckInfrastructureEvent;
import de.derteufelqwe.ServerManager.spring.events.ReloadConfigEvent;
import de.derteufelqwe.commons.Constants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Log4j2
public class SpringEventHandler {

    @Autowired
    private ApplicationContext appContext;
    @Autowired
    private Docker docker;
    @Autowired
    private Commons commons;


    /**
     * Startup code
     *
     * @param event
     */
    @EventListener
    public void onStart(ApplicationStartedEvent event) {
        if (ServerManager.SKIP_STARTUP_CHECKS)
            return;

        this.pullImages();
        if (!commons.checkAndCreateInfrastructure()) {
            log.error("Infrastructure setup failed!");
            SpringApplication.exit(appContext, () -> 100);
        }
        if (!commons.checkAndCreateMCServers()) {
            log.fatal("Minecraft server setup failed!");
            SpringApplication.exit(appContext, () -> 102);
        }
    }


    private void pullImages() {
        long duration = 360;

        log.info("Downloading required images. This can take a few minutes.");
        ImagePullCallback callback1 = docker.pullImage(Constants.Images.REGISTRY);
        ImagePullCallback callback2 = docker.pullImage(Constants.Images.OPENSSL);
        ImagePullCallback callback3 = docker.pullImage(Constants.Images.HTPASSWD);
        ImagePullCallback callback4 = docker.pullImage(Constants.Images.POSTGRES);
        ImagePullCallback callback5 = docker.pullImage(Constants.Images.REDIS);
        ImagePullCallback callback6 = docker.pullImage(Constants.Images.DECKSCHRUBBER);

        try {
            callback1.awaitCompletion(duration, TimeUnit.SECONDS);
            callback2.awaitCompletion(duration, TimeUnit.SECONDS);
            callback3.awaitCompletion(duration, TimeUnit.SECONDS);
            callback4.awaitCompletion(duration, TimeUnit.SECONDS);
            callback5.awaitCompletion(duration, TimeUnit.SECONDS);
            callback6.awaitCompletion(duration, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            log.error("Image download interrupted: {}.", e.getMessage());
            log.error("Fix this error! Otherwise DockerMC won't work properly.");
            return;
        }

        log.info("Downloaded all required images.");
    }

}
