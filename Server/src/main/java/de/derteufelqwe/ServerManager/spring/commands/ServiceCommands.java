package de.derteufelqwe.ServerManager.spring.commands;

import com.github.dockerjava.api.command.HealthStateLog;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.spring.Commons;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.ServerManager.utils.HelpBuilder;
import de.derteufelqwe.ServerManager.utils.ServiceHealthReader;
import de.derteufelqwe.ServerManager.utils.Utils;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.extern.log4j.Log4j2;
import org.jline.reader.LineReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

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
    @Autowired @Lazy
    private LineReader lineReader;
    @Autowired
    private Commons commons;


    @ShellMethod(value = "Shows the help", key = "service")
    private void showHelp() {
        System.out.println("Manage services");
        System.out.println("");

        new HelpBuilder("Commands:")
                .addEntry("help", "Shows this help")
                .addEntry("list", "Lists all available services")
                .addEntry("details", "Displays details about a certain service")
                .addEntry("create", "Creates / updates the MC / BC services from the config")
                .addEntry("stop", "Stops / removes MC / BC services and their containers")
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

            ServiceModeConfig modeConfig = service.getSpec().getMode();
            long maxReplicas = -1;
            if (modeConfig.getReplicated() != null)
                maxReplicas = modeConfig.getReplicated().getReplicas();
            int runningReplicas = getRunningReplicas(service.getId());
            ServiceHealthReader.ServiceHealth health = healthReader.getHealth(service.getId());

            tableBuilder.addToColumn(0, name);
            tableBuilder.addToColumn(1, service.getId());
            if (maxReplicas >= 0)
                tableBuilder.addToColumn(2, String.format("%s/%s", runningReplicas, maxReplicas));
            else
                tableBuilder.addToColumn(2, String.format("%s (g)", runningReplicas));

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

        ServiceModeConfig serviceModeConfig = service.getSpec().getMode();
        long maxReplicas = -1;
        if (serviceModeConfig.getReplicated() != null)
            maxReplicas = serviceModeConfig.getReplicated().getReplicas();
        int runningReplicas = getRunningReplicas(service.getId());

        // --- Log the infos ---
        tableBuilder.addToColumn(0, "ID");
        tableBuilder.addToColumn(1, service.getId());

        tableBuilder.addToColumn(0, "Name");
        tableBuilder.addToColumn(1, service.getSpec().getName());

        tableBuilder.addToColumn(0, "Updated");
        tableBuilder.addToColumn(1, service.getUpdatedAt().toString());

        tableBuilder.addToColumn(0, "Replicas");
        if (maxReplicas >= 0)
            tableBuilder.addToColumn(1, String.format("%s/%s", runningReplicas, maxReplicas));
        else
            tableBuilder.addToColumn(1, String.format("%s (g)", runningReplicas));

        tableBuilder.addToColumn(0, "Health");
        if (health.isHealthy())
            tableBuilder.addToColumn(1, "(healthy)");
        else
            tableBuilder.addToColumn(1, health.getLogs());

        tableBuilder.build(log);
    }

    @ShellMethod(value = "Reloads and updates the servers config.", key = "service create")
    public void create(
            @ShellOption({"-a", "--all"}) boolean all,
            @ShellOption({"-rc", "--reloadConfig"}) boolean reloadConfig,
            @ShellOption({"-f", "--force"}) boolean force,
            @ShellOption({"-b", "--bungee"}) boolean updateBungee,
            @ShellOption({"-l", "--lobby"}) boolean updateLobby,
            @ShellOption({"-p", "--pool"}) boolean updatePool
    ) {
        // Reload the config if requrested
        if (reloadConfig) {
            if (commons.reloadServerConfig()) {
                log.info("Reloaded server config.");

            } else {
                log.error("Server config reload failed. Solve the error and rerun the command.");
                return;
            }
        }

        // Update everything at default
        if (all) {
            if (commons.createAllMCServers(force)) {
                log.info("Successfully reloaded server config.");

            } else {
                log.error("Config reload failed!");
            }

            return;
        }

        // Handle the special cases
        if (updateBungee)
            commons.createBungeeServer(force);

        if (updateLobby)
            commons.createLobbyServer(force);

        if (updatePool)
            commons.createPoolServers(force);

    }

    @ShellMethod(value = "Stops ALL Minecraft and BungeeCord server.", key = {"service stop"})
    public void stop(
            @ShellOption({"-a", "--all"}) boolean all,
            @ShellOption({"-b", "--bungee"}) boolean stopBungee,
            @ShellOption({"-l", "--lobby"}) boolean stopLobby,
            @ShellOption(value = {"-p", "--pool"}, defaultValue = "") List<String> poolNames
    ) {
        // Default action
        if (all) {
            log.warn("You are about to stop ALL Minecraft and BungeeCord server, kicking all players in the process. Are you sure? (Y/N)");
            String input = lineReader.readLine("> ").toUpperCase();

            if (!input.equals("Y")) {
                log.info("Server shutdown cancelled.");
                return;
            }

            commons.stopAllMCServers();
            log.info("Successfully stopped all Minecraft and BungeeCord services.");
            return;
        }

        // Specific actions
        if (stopBungee)
            commons.stopBungeeServer();

        if (stopLobby)
            commons.stopLobbyServer();

        for (String poolName : poolNames) {
            commons.stopPoolServer(poolName);
        }

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
