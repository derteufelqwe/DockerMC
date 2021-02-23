package de.derteufelqwe.nodewatcher.misc;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.annotation.CheckForNull;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

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

                List<String> res = (List<String>) session.createNativeQuery(
                        "SELECT id FROM containers WHERE stoptime IS NULL and node_id = :nodeid")
                        .setParameter("nodeid", node.getId())
                        .getResultList();

                if (res == null)
                    return resSet;

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
    @CheckForNull
    public static Timestamp parseDockerTimestamp(String timeString) {
        String rightLength = timeString.substring(0, 23);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
//        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            return new Timestamp(format.parse(rightLength).getTime());

        } catch (ParseException e) {
            return null;
        }
    }

}
