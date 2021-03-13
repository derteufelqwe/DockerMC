package de.derteufelqwe.ServerManager.utils;

import com.github.dockerjava.api.command.HealthStateLog;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import lombok.Getter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class to retrieve health information of services
 */
public class ServiceHealthReader {

    private final Pattern RE_CLEAN_CURL = Pattern.compile("(.+% Total +% Received +% Xferd +Average +Speed +Time +Time +Time +Current .+curl: \\(\\d+\\) )(.+)");

    private Docker docker;


    public ServiceHealthReader(Docker docker) {
        this.docker = docker;
    }


    /**
     * Returns the health of a service
     * @param serviceID
     * @return
     * @throws NotFoundException
     */
    public ServiceHealth getHealth(String serviceID) throws NotFoundException {
        Service service = docker.getDocker().inspectServiceCmd(serviceID).exec();
        ServiceHealth health = new ServiceHealth();

        health.addLogs(analyzeHealth(service));
        health.addLogs(analyzeTasks(service));

        return health;
    }

    /**
     * Analyzes the health checks of containers and returns a set of their error outputs
     *
     * @param containers
     * @return
     */
    private Set<String> parseContainerHealthLogs(List<InspectContainerResponse> containers) {
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
                    .map(this::cleanLogMessage)
                    .collect(Collectors.toSet());

            logs.addAll(healthLogs);
        }

        return logs;
    }

    /**
     * Analyzes the health of the containers and returns a set of their error messages
     * @param service
     * @return
     */
    private Set<String> analyzeHealth(Service service) {
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

        return this.parseContainerHealthLogs(inspectContainerResponses);
    }

    /**
     * Analyzes the errors of the services tasks
     * @param service
     * @return
     */
    private Set<String> analyzeTasks(Service service) {
        List<Task> tasks = docker.getDocker().listTasksCmd()
                .withServiceFilter(service.getId())
                .withStateFilter(TaskState.RUNNING, TaskState.ACCEPTED)
                .exec();

        return this.getTaskStates(tasks);
    }

    /**
     * Used to clean common error log messages.
     * @param message Message to stream.
     * @return
     */
    private String cleanLogMessage(String message) {
        Matcher m = RE_CLEAN_CURL.matcher(message);

        // Remove irrelevant curl text
        if (m.matches()) {
            message = m.group(2);
        }

        return message;
    }

    /**
     * Analyzes the task errors for more errors
     *
     * @param tasks
     * @return
     */
    private Set<String> getTaskStates(List<Task> tasks) {
        return tasks.stream()
                .map(Task::getStatus)
                .filter(Objects::nonNull)
                .map(TaskStatus::getErr)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }


    public static class ServiceHealth {

        private boolean forceUnhealthy = false;
        @Getter
        private Set<String> logs = new HashSet<>();


        public ServiceHealth() {

        }

        public void addLog(String message) {
            logs.add(message);
        }

        public void  addLogs(Collection<? extends String> collection) {
            logs.addAll(collection);
        }

        public void setForceUnhealthy(boolean forceUnhealthy) {
            this.forceUnhealthy = forceUnhealthy;
        }

        public boolean isHealthy() {
            return logs.size() == 0 && !forceUnhealthy;
        }

    }
}
