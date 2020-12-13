package de.derteufelqwe.nodewatcher.misc;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.annotation.CheckForNull;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class NWUtils {

    /**
     * Returns a synchronized Set containing the ids of all containers, which are running on the local docker swarm node
     * @return
     */
    public static Set<String> findLocalRunningContainers(SessionBuilder sessionBuilder) {
        Set<String> resSet = Collections.synchronizedSet(new HashSet<>());

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                // --- Create the query criteria ---
                Node node = session.get(Node.class, NodeWatcher.getSwarmNodeId());
                if (node == null) {
                    throw new InvalidSystemStateException("Failed to find node object for %s!", NodeWatcher.getSwarmNodeId());
                }

                CriteriaBuilder cb = session.getCriteriaBuilder();
                CriteriaQuery<DBContainer> cq = cb.createQuery(DBContainer.class);
                Root<DBContainer> root = cq.from(DBContainer.class);

                cq.select(root).where(cb.isNull(root.get("stopTime")), cb.equal(root.get("node"), node));

                // --- Execute the query ---
                Query queryRes = session.createQuery(cq);
                List<DBContainer> res = (List<DBContainer>) queryRes.getResultList();

                for (DBContainer container : res) {
                    resSet.add(container.getId());
                }

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
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            return new Timestamp(format.parse(rightLength).getTime());

        } catch (ParseException e) {
            System.err.println("Failed to parse timestamp '" + timeString + "'.");
            return null;
        }
    }

}
