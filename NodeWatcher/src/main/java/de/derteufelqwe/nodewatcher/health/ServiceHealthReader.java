package de.derteufelqwe.nodewatcher.health;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Task;
import com.github.dockerjava.api.model.TaskState;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.LocalSessionRunnable;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.TypedSessionRunnable;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.DBServiceHealth;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.nodewatcher.DBQueries;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.executors.ContainerEventHandler;
import de.derteufelqwe.nodewatcher.misc.NWUtils;
import de.derteufelqwe.nodewatcher.misc.RepeatingThread;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.EntityNotFoundException;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Responsible for periodically downloading the new logs for a container.
 * Containers are added by {@link ContainerEventHandler}
 */
@Log4j2
public class ServiceHealthReader extends RepeatingThread {

    private final Pattern RE_LOGDRIVER_CONTAINER_ID = Pattern.compile("(error creating logger: Failed to find container) .+ (in the DB)");
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();
    private final DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();


    public ServiceHealthReader() {
        super(10);
    }

    @Override
    public void repeatedRun() {
        try {
            // Find all relevant services
            List<String> serviceIDs = this.getRelevantServiceIDs();
            log.info("Updating healths for {} services.", serviceIDs.size());

            for (String serviceID : serviceIDs) {
                List<Task> tasks = dockerClient.listTasksCmd()
                        .withServiceFilter(serviceID)
                        .exec();

                this.fetchServiceHealth(serviceID, tasks);
                this.completeCompletedTasks(serviceID, tasks);
            }

        } catch (Exception e) {
            log.error("ServiceHealthReader caught exception.", e);
            CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e, NodeWatcher.getMetaData());
        }
    }

    // -----  Utility methods  -----

    private List<String> getRelevantServiceIDs() {
        return new TypedSessionRunnable<List<String>>(sessionBuilder) {
            @Override
            protected List<String> exec(Session session) {
                return DBQueries.getActiveServicesIDs(session);
            }
        }.run();
    }

    /**
     * Fetches the running tasks and creates a database entry for these
     *
     * @param serviceID
     */
    private void fetchServiceHealth(String serviceID, List<Task> tasks) {

        new LocalSessionRunnable(sessionBuilder) {
            @Override
            protected void exec(Session session) {
                try {
                    DBService dbService = session.getReference(DBService.class, serviceID);

                    for (Task task : tasks) {
                        try {
                            Timestamp timestamp = NWUtils.parseDockerTimestamp(task.getCreatedAt());
                            TaskState taskState = task.getStatus().getState();
                            String nodeID = task.getNodeId();

                            // Node can be null when a task can't even be scheduled on a node due to placement constraints
                            Node node = nodeID == null ? null : session.getReference(Node.class, nodeID);

                            DBServiceHealth dbServiceHealth = new DBServiceHealth(
                                    task.getId(),
                                    dbService,
                                    node,
                                    timestamp,
                                    sanitizeLogMessage(task.getStatus().getErr()),
                                    parseTaskState(taskState)
                            );

                            session.saveOrUpdate(dbServiceHealth);

                        } catch (EntityNotFoundException e) {
                            log.error("Failed to save health {}. Node {} not found.", task.getId(), task.getNodeId());
                        }
                    }

                } catch (EntityNotFoundException e) {
                    log.error("Service health fetch failed. Service {} not found.", serviceID);
                }
            }
        }.run();
    }

    /**
     * Sets the taskstates of all DBServiceHealths to completed if they are not present in the tasks anymore
     *
     * @param serviceID
     * @param tasks
     */
    private void completeCompletedTasks(String serviceID, List<Task> tasks) {
        List<String> availableTaskIDs = tasks.stream()
                .map(Task::getId)
                .collect(Collectors.toList());

        new LocalSessionRunnable(sessionBuilder) {
            @Override
            protected void exec(Session session) {
                List<DBServiceHealth> runningTasks = DBQueries.getAllRunningTasks(session, serviceID);

                for (DBServiceHealth task : runningTasks) {
                    if (!availableTaskIDs.contains(task.getTaskID())) {
                        task.setTaskState(DBServiceHealth.TaskState.COMPLETE);
                        session.update(task);
                    }
                }

            }
        }.run();
    }

    @NotNull
    private String sanitizeLogMessage(@Nullable String message) {
        if (message == null) {
            return "";
        }

        Matcher m1 = RE_LOGDRIVER_CONTAINER_ID.matcher(message);
        if (m1.find()) {
            message = m1.replaceFirst("$1 $2");
        }

        return message;
    }

    private DBServiceHealth.TaskState parseTaskState(TaskState state) {
        try {
            return DBServiceHealth.TaskState.valueOf(state.getValue().toUpperCase());

        } catch (IllegalArgumentException e) {
            return DBServiceHealth.TaskState.UNKNOWN;
        }
    }

}
