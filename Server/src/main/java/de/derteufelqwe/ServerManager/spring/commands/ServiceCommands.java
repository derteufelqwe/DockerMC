package de.derteufelqwe.ServerManager.spring.commands;

import com.github.dockerjava.api.command.HealthStateLog;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.ServerManager.utils.HelpBuilder;
import de.derteufelqwe.ServerManager.utils.ServiceHealthReader;
import de.derteufelqwe.ServerManager.utils.Utils;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ShellComponent
@Log4j2
public class ServiceCommands {

    @Autowired
    private Docker docker;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private SessionBuilder sessionBuilder;
    @Autowired
    private ServiceHealthReader healthReader;


    @ShellMethod(value = "Shows the help", key = "service")
    private void showHelp() {
        System.out.println("Manage services");
        System.out.println("");

        new HelpBuilder("Commands:")
                .addEntry("help", "Shows this help")
                .addEntry("list", "Lists all available services")
                .addEntry("details", "Displays details about a certain service")
                .print();
    }

    @ShellMethod(value = "Shows the help", key = "service help")
    private void showHelp2() {
        showHelp();
    }


    @ShellMethod(value = "Lists all running BungeeCord and Minecraft server", key = "service list")
    public void listServices() {
        String lobbyServerName = redisTemplate.opsForValue().get(Constants.REDIS_KEY_LOBBYSERVER);

        List<Service> services = docker.getDocker().listServicesCmd()
                .withLabelFilter(Collections.singletonMap(Constants.DOCKER_IDENTIFIER_KEY, Constants.DOCKER_IDENTIFIER_VALUE))
                .exec();

        TableBuilder tableBuilder = new TableBuilder()
                .withNoRowSeparation()
                .withColumn(new Column.Builder()
                        .withTitle("Name")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("ID")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Replicas")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Health")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Type")
                        .build());

        for (Service service : services) {
            String name = service.getSpec().getName();
            String lobbyAppend = name.equals(lobbyServerName) ? " (LobbyServer)" : "";
            String type = service.getSpec().getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY);

            long maxReplicas = service.getSpec().getMode().getReplicated().getReplicas();
            int runningReplicas = getRunningReplicas(service.getId());
            ServiceHealthReader.ServiceHealth health = healthReader.getHealth(service.getId());

            tableBuilder.addToColumn(0, name);
            tableBuilder.addToColumn(1, service.getId());
            tableBuilder.addToColumn(2, String.format("%s/%s", runningReplicas, maxReplicas));
            if (health.isHealthy())
                tableBuilder.addToColumn(3, "Healthy");
            else
                tableBuilder.addToColumn(3, "Unhealthy");
            tableBuilder.addToColumn(4, type + lobbyAppend);
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

        TableBuilder tableBuilder = new TableBuilder()
                .withColumn(new Column.Builder()
                        .withTitle("Attribute")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Value")
                        .build());

        ServiceHealthReader.ServiceHealth health = healthReader.getHealth(service.getId());

        long maxReplicas = service.getSpec().getMode().getReplicated().getReplicas();
        int runningReplicas = getRunningReplicas(service.getId());

        // --- Log the infos ---
        tableBuilder.addToColumn(0, "ID");
        tableBuilder.addToColumn(1, service.getId());

        tableBuilder.addToColumn(0, "Name");
        tableBuilder.addToColumn(1, service.getSpec().getName());

        tableBuilder.addToColumn(0, "Updated");
        tableBuilder.addToColumn(1, service.getUpdatedAt().toString());

        tableBuilder.addToColumn(0, "Replicas");
        tableBuilder.addToColumn(1, String.format("%s/%s", runningReplicas, maxReplicas));

        tableBuilder.addToColumn(0, "Health");
        if (health.isHealthy())
            tableBuilder.addToColumn(1, "(healthy)");
        else
            tableBuilder.addToColumn(1, health.getLogs());

        tableBuilder.build(log);
    }


    /**
     * Returns the amount of tasks of a service that are currently running.
     *
     * @return
     */
    private int getRunningReplicas(String serviceId) {
        List<Task> tasks = docker.getDocker().listTasksCmd()
                .withServiceFilter(serviceId)
                .withStateFilter(TaskState.RUNNING)
                .exec().stream()
                .filter(t -> t.getStatus().getState().equals(TaskState.RUNNING))
                .collect(Collectors.toList());

        return tasks.size();
    }

}
