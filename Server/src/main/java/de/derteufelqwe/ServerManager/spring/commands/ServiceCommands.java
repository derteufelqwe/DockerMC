package de.derteufelqwe.ServerManager.spring.commands;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.ServerManager.utils.Utils;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ShellComponent
@Log4j2
@ShellCommandGroup(value = "service")
public class ServiceCommands {

    @Autowired
    private Docker docker;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private SessionBuilder sessionBuilder;


    @ShellMethod(value = "Lists all running BungeeCord and Minecraft server", key = "service list")
    public void listServices() {
        String lobbyServerName = redisTemplate.opsForValue().get(Constants.REDIS_KEY_LOBBYSERVER);

        List<Service> services = docker.getDocker().listServicesCmd()
                .withLabelFilter(Collections.singletonMap(Constants.DOCKER_IDENTIFIER_KEY, Constants.DOCKER_IDENTIFIER_VALUE))
                .exec();

        TableBuilder tableBuilder = new TableBuilder()
                .withColumn(new Column.Builder()
                        .withTitle("Name")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("ID")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Type")
                        .build());

        for (Service service : services) {
            String name = service.getSpec().getName();
            String lobbyAppend = name.equals(lobbyServerName) ? " (LobbyServer)" : "";
            String type = service.getSpec().getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY);

            tableBuilder.addToColumn(0, name);
            tableBuilder.addToColumn(1, service.getId());
            tableBuilder.addToColumn(2, type + lobbyAppend);
        }

        tableBuilder.build(log);
    }

    @ShellMethod(value = "Shows service details", key = "service details")
    public void serviceDetails(String serviceId) {
        Service service;
        try {
            service = docker.getDocker().inspectServiceCmd(serviceId).exec();

        } catch (NotFoundException e) {
            log.error("Service {} not found.", serviceId);
            return;
        }

        String currentNodeID = Utils.getLocalSwarmNode(docker);

        Map<String, String> filters = new HashMap<>();
        filters.put("com.docker.swarm.node.id", currentNodeID);
        filters.put("com.docker.swarm.service.id", service.getId());

        List<Container> containers = docker.getDocker().listContainersCmd()
                .withLabelFilter(filters)
                .withShowAll(true)
                .exec();
        containers = containers.subList(0, Math.min(containers.size(), 5));

        List<InspectContainerResponse> inspectContainerResponses = containers.stream()
                .map(c -> docker.getDocker().inspectContainerCmd(c.getId()).exec())
                .collect(Collectors.toList());

        // Log the info
        log.info("ID: {}", service.getId());
        log.info("Name: {}", service.getSpec().getName());
        log.info("Updated: {}", service.getUpdatedAt());
        log.info("Wanted replicas: {}", service.getSpec().getMode().getReplicated().getReplicas());

    }

}
