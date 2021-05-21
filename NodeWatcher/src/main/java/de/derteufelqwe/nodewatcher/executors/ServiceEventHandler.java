package de.derteufelqwe.nodewatcher.executors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.exceptions.DockerAPIIncompleteException;
import de.derteufelqwe.commons.exceptions.DockerMCException;
import de.derteufelqwe.commons.hibernate.LocalSessionRunnable;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.nodewatcher.NodeWatcher;

import de.derteufelqwe.nodewatcher.misc.*;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Watches for docker service events to update the services in the database.
 */
@Log4j2
public class ServiceEventHandler implements ResultCallback<Event> {

    /**
     * Time in ms in which the second event for the same service gets blocked
     */
    public final int EVENT_BLOCK_INTERVAL = 200;

    private final DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();

    /**
     * Indicates that the NodeEventHandler was started and all nodes are present in the DB
     */
    private final CountDownLatch hasStartedLatch = new CountDownLatch(1);
    private Set<Event> recentEvents = new HashSet<>();
    private Set<IServiceObserver> observers = new HashSet<>();


    public ServiceEventHandler() {

    }


    /**
     * Initializes the ContainerWatcher and makes sure that all running containers are stored in the database
     * and containers the stopped while this ContainerWatcher was offline, are updated accordingly
     */
    @Override
    public void onStart(Closeable closeable) {
        try {
            List<Service> runningServices = NWUtils.getRunningMCBCServices(dockerClient);
            this.finishStoppedServices(runningServices);
            this.updateRunningServices(runningServices);
            this.createNewServices(runningServices);

            this.hasStartedLatch.countDown();

        } catch (Exception e) {
            CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e, NodeWatcher.getMetaData());
            throw e;
        }
    }

    @Override
    public void onNext(Event object) {
        try {
            EventActor actor = object.getActor();

            if (object.getAction() == null) {
                log.warn("Got event without action: {}.", object);
                return;
            }
            if (actor == null) {
                log.warn("Got event without actor: {}.", object);
                return;
            }
            if (hadEventRecently(object)) {
                return;
            }

            switch (object.getAction()) {
                case "create":
                    this.onServiceCreated(actor.getId());
                    break;

                case "remove":
                    this.onServiceRemoved(actor.getId());
                    break;

                case "update":
                    this.onServiceUpdated(actor.getId());
                    break;

                default:
                    log.error("Got invalid event type " + object);
            }

            this.recentEvents.add(object);

        } catch (Exception e) {
            log.error("Exception occurred in the ContainerWatcher.");
            log.error(e.getMessage());
            e.printStackTrace(System.err);
            CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e, NodeWatcher.getMetaData());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        log.error("Uncaught exception occurred in the ServiceEventHandler.", throwable);
    }

    @Override
    public void onComplete() {

    }

    @Override
    public void close() throws IOException {

    }

    // -----  Custom methods  -----

    public void addObserver(IServiceObserver observer) {
        this.observers.add(observer);
    }

    /**
     * Compares the running services in the DB with the ones actually running and finishes the database records for removed
     * services
     * @param runningServices
     */
    private void finishStoppedServices(List<Service> runningServices) {
        List<String> serviceIDs = runningServices.stream()
                .map(Service::getId)
                .collect(Collectors.toList());

        Set<String> toFinishIDs = new HashSet<>();

        new LocalSessionRunnable(sessionBuilder) {
            @Override
            protected void exec(Session session) {
                List<DBService> dbServices = session.createQuery(
                        "SELECT s FROM DBService AS s WHERE s.active=true",
                        DBService.class).getResultList();

                for (DBService dbService : dbServices) {
                    // Service not running anymore
                    if (!serviceIDs.contains(dbService.getId())) {
                        toFinishIDs.add(dbService.getId());
                    }
                }
            }
        }.run();

        // Remove the services marked for removal
        for (String id : toFinishIDs) {
            this.onServiceRemoved(id);
        }
    }

    /**
     * Compares the running services in the DB with the ones actually running and updates the database records
     * @param runningServices
     */
    private void updateRunningServices(List<Service> runningServices) {
        List<String> serviceIDs = runningServices.stream()
                .map(Service::getId)
                .collect(Collectors.toList());

        Set<String> toUpdateIDs = new HashSet<>();

        new LocalSessionRunnable(sessionBuilder) {
            @Override
            protected void exec(Session session) {
                List<DBService> dbServices = session.createQuery(
                        "SELECT s FROM DBService AS s WHERE s.active=true",
                        DBService.class).getResultList();

                for (DBService dbService : dbServices) {
                    // Service not running anymore
                    if (serviceIDs.contains(dbService.getId())) {
                        toUpdateIDs.add(dbService.getId());
                    }
                }
            }
        }.run();

        // Update the services marked for updating
        for (String id : toUpdateIDs) {
            this.onServiceUpdated(id);
        }
    }

    /**
     * Compares the running services in the DB with the ones actually running and creates the services in the DB, which are running
     * but not present in the DB
     * @param runningServices
     */
    private void createNewServices(List<Service> runningServices) {
        List<String> serviceIDs = runningServices.stream()
                .map(Service::getId)
                .collect(Collectors.toList());

        Set<String> dbExistingIDs = new HashSet<>();

        sessionBuilder.execute(session -> {
            List<DBService> dbServices = session.createQuery(
                    "SELECT s FROM DBService AS s WHERE s.active = true",
                    DBService.class).getResultList();

            for (DBService dbService : dbServices) {
                dbExistingIDs.add(dbService.getId());
            }
        });


        // Created the services, which are not present in the database
        for (String id : serviceIDs) {
            if (!dbExistingIDs.contains(id)) {
                this.onServiceCreated(id);
            }
        }
    }

    /**
     * Checks if a service has fired an event recently (mx 100ms ago) since the docker api follows the create / update event
     * with an additional update event which is irrelevant for this
     * @param event
     * @return
     */
    @SuppressWarnings("ConstantConditions")
    private boolean hadEventRecently(Event event) {
        recentEvents.removeIf(e -> System.currentTimeMillis() >= (e.getTimeNano() / 1_000_000) + EVENT_BLOCK_INTERVAL);

        for (Event ev : recentEvents) {
            if (ev.getActor().getId().equals(event.getActor().getId())) {
                return true;
            }
        }

        return false;
    }

    private void onServiceCreated(String id) {
        this.createOrUpdateService(id, true);
    }

    private void onServiceUpdated(String id) {
        this.createOrUpdateService(id, false);
    }

    private void onServiceRemoved(String id) {
        new LocalSessionRunnable(sessionBuilder) {
            @Override
            protected void exec(Session session) {
                DBService dbService = session.get(DBService.class, id);
                if (dbService == null) {
                    log.warn("Stopped service {} not found. Maybe it's not from DockerMC?", id);
                    return;
                }

                dbService.setActive(false);

                session.update(dbService);
            }
        }.run();

        log.info("Finished service {}.", id);
        for (IServiceObserver observer : this.observers) {
            observer.onServiceStop(id);
        }
    }

    /**
     *
     * @param id
     * @param created true = service created, false = service updated
     */
    private void createOrUpdateService(String id, boolean created) {
        Service service = dockerClient.inspectServiceCmd(id)
                .exec();

        if (!isFromDockerMC(service)) {
            log.debug("Handling non DockerMC service {}. Ignoring it.", id);
            return;
        }

        new LocalSessionRunnable(sessionBuilder) {
            @Override
            public void exec(Session session) {

                try {
                    DBService dbService = new DBService(
                            service.getId(),
                            getName(service),
                            getMemoryLimit(service),
                            getCPULimit(service),
                            getTypeLabel(service),
                            getReplicaCount(service)
                    );

                    session.saveOrUpdate(dbService);

                } catch (DockerAPIIncompleteException e) {
                    log.error("Docker API returned incomplete data.", e);
                }
            }
        }.run();

        if (created) {
            log.info("Created service {}.", id);
            for (IServiceObserver observer : this.observers) {
                observer.onServiceStart(id);
            }

        } else {
            log.debug("Updated service {}.", id);
        }
    }

    // -----  Utility methods  -----

    public boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
        return this.hasStartedLatch.await(timeout, unit);
    }

    @SuppressWarnings("ConstantConditions")
    private String getName(Service service) {
        try {
            ServiceSpec serviceSpec = service.getSpec();
            return serviceSpec.getName();

        } catch (NullPointerException e) {
            throw new DockerMCException(e, "Service %s has no name. Service: %s.", service.getId(), service);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private int getMemoryLimit(Service service) {
        try {
            ServiceSpec serviceSpec = service.getSpec();
            TaskSpec taskSpec = serviceSpec.getTaskTemplate();
            ResourceRequirements resourceRequirements = taskSpec.getResources();
            ResourceSpecs resourceSpecs = resourceRequirements.getLimits();
            return new Long((long) (resourceSpecs.getMemoryBytes() / 1024.0 / 1024.0)).intValue();


        } catch (NullPointerException e) {
            throw new DockerMCException(e, "Service %s has no memory limit. Service: %s.", service.getId(), service);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private float getCPULimit(Service service) {
        try {
            ServiceSpec serviceSpec = service.getSpec();
            TaskSpec taskSpec = serviceSpec.getTaskTemplate();
            ResourceRequirements resourceRequirements = taskSpec.getResources();
            ResourceSpecs resourceSpecs = resourceRequirements.getLimits();
            return (float) (resourceSpecs.getNanoCPUs() / 1000000000.0);

        } catch (NullPointerException e) {
            throw new DockerMCException(e, "Service %s has no CPU limit. Service: %s.", service.getId(), service);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    private Map<String, String> getLabels(Service service) {
        try {
            ServiceSpec serviceSpec = service.getSpec();
            Map<String, String> labels = serviceSpec.getLabels();
            if (labels != null) {
                return labels;

            } else {
                throw new NullPointerException("Labels not found.");
            }

        } catch (NullPointerException e) {
            throw new DockerMCException(e, "Service %s has no Labels. Service: %s.", service.getId(), service);
        }
    }

    private String getTypeLabel(Service service) {
        Map<String, String> labels = getLabels(service);
        return labels.get(Constants.CONTAINER_IDENTIFIER_KEY);
    }

    /**
     * Checks if a service, which issues the event is actually an event from DockerMC
     * @param service
     * @return
     */
    private boolean isFromDockerMC(Service service) {
        String type = getLabels(service).get(Constants.DOCKER_IDENTIFIER_KEY);
        return type != null && type.equals(Constants.DOCKER_IDENTIFIER_VALUE);
    }

    private int getReplicaCount(Service service) {
        try {
            return (int) service.getSpec()
                    .getMode()
                    .getReplicated()
                    .getReplicas();

        } catch (NullPointerException e) {
            log.error("Service {} is not in replicated mode.", service.getId(), e);
        }

        return -1;
    }

}
