package de.derteufelqwe.ServerManager.utils;

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

    private final Pattern RE_LOGDRIVER_CONTAINER_ID = Pattern.compile("(error creating logger: Failed to find container) .+ (in the DB)");

    private Docker docker;


    public ServiceHealthReader(Docker docker) {
        this.docker = docker;
    }


    /**
     * Returns the health of a service
     *
     * @param serviceID
     * @return
     * @throws NotFoundException
     */
    public ServiceHealth getHealth(String serviceID) throws NotFoundException {
        ServiceHealth health = new ServiceHealth();

        Service service = docker.getDocker().inspectServiceCmd(serviceID).exec();
        List<Task> tasks = docker.getDocker().listTasksCmd()
                .withServiceFilter(service.getId())
                .exec();

        List<Task> runningTasks = tasks.stream()
                .filter(t -> t.getStatus().getState() == TaskState.RUNNING)
                .collect(Collectors.toList());

        health.addLogs(getTaskStates(tasks));
        health.addLogs(analyzeRunningTaskCount(runningTasks.size(), service));
        health.addLogs(analyzeHealthcheckLogs(serviceID));

        return health;
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
                .map(this::sanitizeLogMessage)
                .map(t -> "T: " + t)
                .collect(Collectors.toSet());
    }

    /**
     * Analyzes if all required tasks are up and running.
     *
     * @return
     */
    private Set<String> analyzeRunningTaskCount(int taskCount, Service service) {
        Set<String> logs = new HashSet<>();

        ServiceModeConfig modeConfig = service.getSpec().getMode();
        long maxReplicas = -1;
        if (modeConfig.getReplicated() != null) {
            maxReplicas = modeConfig.getReplicated().getReplicas();

        } else {
            return logs;
        }

        if (taskCount < maxReplicas) {
            logs.add(String.format("R: Too little tasks running. (%s/%s)", taskCount, maxReplicas));
        }
        if (taskCount > maxReplicas) {
            logs.add(String.format("R: Too many tasks running. (%s/%s)", taskCount, maxReplicas));
        }

        return logs;
    }

    private Set<String> analyzeHealthcheckLogs(String serviceID) {
        Set<String> logs = new HashSet<>();


        return logs;
    }

    private String sanitizeLogMessage(String message) {
        Matcher m1 = RE_LOGDRIVER_CONTAINER_ID.matcher(message);
        if (m1.find()) {
            message = m1.replaceFirst("$1 $2");
        }

        return message;
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

        public void addLogs(Collection<? extends String> collection) {
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
