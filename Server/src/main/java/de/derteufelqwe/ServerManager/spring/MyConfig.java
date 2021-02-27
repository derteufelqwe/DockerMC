package de.derteufelqwe.ServerManager.spring;

import de.derteufelqwe.ServerManager.Docker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyConfig {

    @Bean
    public Docker getDocker() {
        return new Docker("tcp", "ubuntu1", 2375);
    }

}
