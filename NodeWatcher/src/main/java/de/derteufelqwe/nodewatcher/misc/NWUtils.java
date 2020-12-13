package de.derteufelqwe.nodewatcher.misc;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Container;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                CriteriaQuery<Container> cq = cb.createQuery(Container.class);
                Root<Container> root = cq.from(Container.class);

                cq.select(root).where(cb.isNull(root.get("stopTime")), cb.equal(root.get("node"), node));

                // --- Execute the query ---
                Query queryRes = session.createQuery(cq);
                List<Container> res = (List<Container>) queryRes.getResultList();

                for (Container container : res) {
                    resSet.add(container.getId());
                }

                return resSet;

            } finally {
                tx.commit();
            }

        }

    }

}
