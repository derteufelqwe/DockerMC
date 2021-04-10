package de.derteufelqwe.nodewatcher.misc;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.exceptions.DockerAPIIncompleteException;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.nodewatcher.DBQueries;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.exceptions.InvalidSystemStateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckForNull;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class NWUtils {

    /**
     * Returns a synchronized Set containing the ids of all containers, which are marked as running on the local
     * docker swarm node in the database
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Set<String> getLocallyRunningContainersFromDB(SessionBuilder sessionBuilder) {
        Set<String> resSet = Collections.synchronizedSet(new HashSet<>());

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                // --- Create the query criteria ---
                Node node = session.get(Node.class, NodeWatcher.getSwarmNodeId());
                if (node == null) {
                    throw new InvalidSystemStateException("Failed to find node object for %s!", NodeWatcher.getSwarmNodeId());
                }

                List<String> res = DBQueries.getRunningContainersOnNode(session, node.getId());
                resSet.addAll(res);

                return resSet;

            } finally {
                tx.commit();
            }

        }

    }

    /**
     * Parses a docker timestamp string into a java timestamp object
     * @param timeString
     * @return
     */
    @NotNull
    public static Timestamp parseDockerTimestamp(String timeString) throws DockerAPIIncompleteException {
        String rightLength = timeString.substring(0, 23);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
//        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            return new Timestamp(format.parse(rightLength).getTime());

        } catch (ParseException e) {
            throw new DockerAPIIncompleteException(e, "Docker object returned unparsable timestamp '%s'.", timeString);
        }
    }

    /**
     * Returns a list of Minecraft and BungeeCord containers currently running or created
     * @param dockerClient
     * @return
     */
    public static List<Container> getRelevantMCBCContainers(DockerClient dockerClient) {
        return dockerClient.listContainersCmd()
                .withLabelFilter(Constants.DOCKER_IDENTIFIER_MAP)
                .exec().stream()
                .filter(c -> c.getLabels() != null)
                .filter(c ->
                        c.getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY).equals(Constants.ContainerType.BUNGEE.name()) ||
                        c.getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY).equals(Constants.ContainerType.MINECRAFT.name()))
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of Minecraft and BungeeCord services currently running.
     * @param dockerClient
     * @return
     */
    public static List<Service> getRunningMCBCServices(DockerClient dockerClient) {
        return dockerClient.listServicesCmd()
                .withLabelFilter(Constants.DOCKER_IDENTIFIER_MAP)
                .exec().stream()
                .filter(s -> s.getSpec() != null)
                .filter(s -> s.getSpec().getLabels() != null)
                .filter(s ->
                        s.getSpec().getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY).equals(Constants.ContainerType.BUNGEE_POOL.name()) ||
                        s.getSpec().getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY).equals(Constants.ContainerType.MINECRAFT_POOL.name()))
                .collect(Collectors.toList());
    }

}
