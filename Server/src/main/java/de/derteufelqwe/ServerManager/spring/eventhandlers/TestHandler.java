package de.derteufelqwe.ServerManager.spring.eventhandlers;

import de.derteufelqwe.ServerManager.spring.events.TestEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TestHandler {

    @EventListener
    public void handleTestEvent(TestEvent event) {
        System.out.println("Got event");
        event.setMsg("gotcha");
    }

}
