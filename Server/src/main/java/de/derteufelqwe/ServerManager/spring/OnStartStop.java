package de.derteufelqwe.ServerManager.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class OnStartStop {

    @Autowired
    private Environment environment;


    @PostConstruct
    public void onStart() {
        System.out.println("Start code");
    }

    @PreDestroy
    public void onStop() {
        System.out.println("Stop code");
    }

}
