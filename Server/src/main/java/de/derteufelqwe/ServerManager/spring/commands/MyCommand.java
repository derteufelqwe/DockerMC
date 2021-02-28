package de.derteufelqwe.ServerManager.spring.commands;

import com.github.dockerjava.api.model.Container;
import de.derteufelqwe.ServerManager.Docker;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.Collections;
import java.util.List;

@ShellComponent
@Log4j2
public class MyCommand {

    @Autowired
    private Docker docker;

    @Autowired
    private StringRedisTemplate redisTemplate;


    @ShellMethod(value = "Test method")
    public void test() {
        System.out.println("hi");
        log.info("info");
        log.warn("warn");
        log.error("error");
    }

    @ShellMethod(value = "Lists all dockermc containers")
    public void listContainers() {
        List<Container> containers = docker.getDocker().listContainersCmd().withLabelFilter(Collections.singletonMap("Owner", "DockerMC")).exec();
        System.out.printf("%-20s | %-30s\n", "ID", "Name");
        System.out.println("----------------------------------------------------");
        for (Container container : containers) {
            System.out.printf("%-20s | %-30s\n",
                    container.getId().substring(0, 20), container.getNames()[0]);
        }
    }


}
