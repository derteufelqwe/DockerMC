package de.derteufelqwe.ServerManager.spring.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * Calling this will result reload the server config and create / update the Minecraft servers
 */
@Getter
@Setter
public class ReloadConfigEvent extends ApplicationEvent {

    private ReloadSource reloadSource;
    private boolean success = true;
    private String message = "";


    public ReloadConfigEvent(Object source, ReloadSource reloadSource) {
        super(source);
        this.reloadSource = reloadSource;
    }


    public enum ReloadSource {
        APPLICATION_START,
        COMMAND;
    }

}



