package de.derteufelqwe.ServerManager.spring.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * Calling this will check if the whole infrastructure is up and running
 */
@Getter
@Setter
public class CheckInfrastructureEvent extends ApplicationEvent {

    private ReloadSource reloadSource;
    private boolean success = true;
    private String message = "";


    public CheckInfrastructureEvent(Object source, ReloadSource reloadSource) {
        super(source);
        this.reloadSource = reloadSource;
    }


    public enum ReloadSource {
        APPLICATION_START,
        COMMAND;
    }

}



