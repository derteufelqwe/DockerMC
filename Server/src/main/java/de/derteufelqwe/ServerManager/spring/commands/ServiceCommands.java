package de.derteufelqwe.ServerManager.spring.commands;

import com.github.dockerjava.api.command.HealthStateLog;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.api.model.Task;
import com.github.dockerjava.api.model.TaskState;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.ServerManager.utils.Utils;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.*;
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
                        .withTitle("Type")
                        .build());

        for (Service service : services) {
            String name = service.getSpec().getName();
            String lobbyAppend = name.equals(lobbyServerName) ? " (LobbyServer)" : "";
            String type = service.getSpec().getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY);

            long maxReplicas = service.getSpec().getMode().getReplicated().getReplicas();
            int runningReplicas = getRunningReplicas(service.getId());

            tableBuilder.addToColumn(0, name);
            tableBuilder.addToColumn(1, service.getId());
            tableBuilder.addToColumn(2, String.format("%s/%s", runningReplicas, maxReplicas));
            tableBuilder.addToColumn(3, type + lobbyAppend);
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

        Set<String> health = this.getHealthLogs(inspectContainerResponses);

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
        if (health.size() == 0)
            tableBuilder.addToColumn(1, "(healthy)");
        else
            tableBuilder.addToColumn(1, health);

        tableBuilder.build(log);
    }



    /**
     * Analyzes the health checks of containers and returns a set of their error outputs
     * @param containers
     * @return
     */
    private Set<String> getHealthLogs(List<InspectContainerResponse> containers) {
        Set<String> logs = new HashSet<>();

        List<InspectContainerResponse.ContainerState> states = containers.stream()
                .map(InspectContainerResponse::getState)
                .filter(s -> !s.getRunning())
                .collect(Collectors.toList());

        if (states.size() == 0)
            return logs;

        for (InspectContainerResponse.ContainerState state : states) {
            // Containers with no health checks have no health property
            if (state.getHealth() == null)
                continue;

            Set<String> healthLogs = state.getHealth().getLog().stream()
                    .filter(l -> l.getExitCodeLong() != 0)
                    .map(HealthStateLog::getOutput)
                    .map(l -> l.replace("\n", ""))
                    .map(l -> l.replace("\r", ""))
                    .collect(Collectors.toSet());

            logs.addAll(healthLogs);
        }

        return logs;
    }

    /**
     * Returns the amount of tasks of a service that are currently running.
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
