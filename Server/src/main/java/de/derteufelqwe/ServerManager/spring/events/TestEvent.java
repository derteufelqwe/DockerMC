package de.derteufelqwe.ServerManager.spring.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class TestEvent extends ApplicationEvent {

    private String msg;

    public TestEvent(Object source, String message) {
        super(source);
    }

}
