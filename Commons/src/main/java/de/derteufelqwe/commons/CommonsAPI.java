package de.derteufelqwe.commons;


import de.derteufelqwe.commons.exceptions.DmcAPIException;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.Notification;
import de.derteufelqwe.commons.hibernate.objects.economy.ServiceBalance;
import de.derteufelqwe.commons.hibernate.objects.permissions.PermissionGroup;
import de.derteufelqwe.commons.misc.ServiceMetaData;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckForNull;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Commons api
 */
public class CommonsAPI {

    private static CommonsAPI instance;


    private CommonsAPI() {

    }


    public static CommonsAPI getInstance() {
        if (instance == null) {
            instance = new CommonsAPI();
        }

        return instance;
    }

    // -----  Players  -----

    @CheckForNull
    public DBPlayer getPlayerFromDB(Session session, String name) throws DmcAPIException {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<DBPlayer> cq = cb.createQuery(DBPlayer.class);
        Root<DBPlayer> root = cq.from(DBPlayer.class);

        cq.select(root).where(cb.equal(root.get("name"), name));

        TypedQuery<DBPlayer> queryRes = session.createQuery(cq);
        List<DBPlayer> res = queryRes.getResultList();

        if (res.size() == 0) {
            return null;

        } else if (res.size() == 1) {
            return res.get(0);

        } else {
            throw new DmcAPIException("Found %s users with the name '%s'.", res.size(), name);
        }

    }

    @CheckForNull
    public DBPlayer getPlayerFromDB(Session session, UUID uuid) {
        return session.get(DBPlayer.class, uuid);
    }

    // -----  Services  -----

    @CheckForNull
    public DBService getActiveServiceFromDB(Session session, String name) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<DBService> cq = cb.createQuery(DBService.class);
        Root<DBService> root = cq.from(DBService.class);

        cq.select(root).where(cb.equal(root.get("name"), name));

        TypedQuery<DBService> queryRes = session.createQuery(cq);
        List<DBService> res = queryRes.getResultList();

        if (res.size() == 0) {
            return null;

        } else if (res.size() == 1) {
            return res.get(0);

        } else {
            throw new DmcAPIException("Found %s active services with the name '%s'.", res.size(), name);
        }
    }

    // -----  Containers  -----

    @CheckForNull
    public DBContainer getContainerFromDB(Session session, String name) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<DBContainer> cq = cb.createQuery(DBContainer.class);
        Root<DBContainer> root = cq.from(DBContainer.class);

        cq.select(root).where(cb.equal(root.get("name"), name));

        TypedQuery<DBContainer> queryRes = session.createQuery(cq);
        List<DBContainer> res = queryRes.getResultList();

        if (res.size() == 0) {
            return null;

        } else if (res.size() == 1) {
            return res.get(0);

        } else {
            throw new DmcAPIException("Found %s containers with the name '%s'.", res.size(), name);
        }
    }

    /**
     * Returns all containers from the DB, which represent a running Minecraft server
     *
     * @return A list that might be empty
     */
    @NotNull
    public List<DBContainer> getRunningMinecraftContainersFromDB(Session session) {
        String sql = "SELECT * FROM containers AS c " +
                "LEFT JOIN services AS s on c.service_id = s.id " +
                "WHERE c.stoptime IS NULL AND s.type = :stype";

        List<DBContainer> containers = session.createNativeQuery(sql, DBContainer.class)
                .setParameter("stype", Constants.ContainerType.MINECRAFT_POOL.name())
                .getResultList();

        if (containers == null)
            return new ArrayList<>();

        return containers;
    }


    /**
     * Returns all containers from the DB, which represent a running BungeeCord proxy
     *
     * @return A list that might be empty
     */
    @NotNull
    public List<DBContainer> getRunningBungeeContainersFromDB(Session session) {
        String sql = "SELECT * FROM containers AS c " +
                "LEFT JOIN services AS s on c.service_id = s.id " +
                "WHERE c.stoptime IS NULL AND s.type = :stype";

        List<DBContainer> containers = session.createNativeQuery(sql, DBContainer.class)
                .setParameter("stype", Constants.ContainerType.BUNGEE_POOL.name())
                .getResultList();

        if (containers == null)
            return new ArrayList<>();

        return containers;
    }


    // -----  Permissions  -----

    @NotNull
    public List<PermissionGroup> getAllPermissionGroups(Session session) {
        List<PermissionGroup> groups = session.createNativeQuery(
                "SELECT * FROM permission_groups", PermissionGroup.class)
                .getResultList();

        if (groups != null)
            return groups;

        return new ArrayList<>();
    }

    @CheckForNull
    public PermissionGroup getPermissionGroup(Session session, String name) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<PermissionGroup> cq = cb.createQuery(PermissionGroup.class);
        Root<PermissionGroup> root = cq.from(PermissionGroup.class);

        cq.select(root).where(
                cb.equal(root.get("name"), name)
        );

        TypedQuery<PermissionGroup> queryRes = session.createQuery(cq);

        try {
            return queryRes.getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }


    public long getPermissionGroupPlayerCount(Session session, String groupName) {
        PermissionGroup group = this.getPermissionGroup(session, groupName);
        if (group == null)
            return -1;

        String SQL = "SELECT COUNT(*) FROM players_permission_groups WHERE permissiongroup_id = :group_id";
        NativeQuery query = session.createSQLQuery(SQL)
                .setParameter("group_id", group.getId());

        Object res = query.getSingleResult();

        return ((BigInteger) res).longValue();
    }


    // -----  Money  -----

    @CheckForNull
    public ServiceBalance getPlayerBalanceOnService(Session session, DBPlayer dbPlayer, DBService dbService) {
        try {
            return session.createNativeQuery("SELECT * FROM service_balance WHERE player_uuid = :pid and service_id = :sid", ServiceBalance.class)
                    .setParameter("pid", dbPlayer.getUuid())
                    .setParameter("sid", dbService.getId())
                    .getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }


    // -----  Notifications  -----

    public long createInfrastructureNotification(Session session, String message) {
        Transaction tx = session.beginTransaction();

        try {
            Notification notification = new Notification("INFRASTRUCTURE", message, null);

            session.persist(notification);

            tx.commit();

            return notification.getId();

        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    /**
     * Serializes the exception into a json structure for the database.
     * @param exception
     * @return
     */
    private Map<String, Object> serializeException(Throwable exception) {
        Map<String, Object> data = new HashMap<>();

        data.put("type", exception.getClass().getSimpleName());
        data.put("message", exception.getMessage());
        data.put("stacktrace", Arrays.stream(exception.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.toList())
        );

        if (exception.getCause() != null) {
            data.put("cause", serializeException(exception.getCause()));
        }

        return data;
    }

    public long createExceptionNotification(Session session, Throwable exception, String containerID, String serviceID, String nodeID) {
        Transaction tx = session.beginTransaction();

        try {
            Map<String, Object> data = serializeException(exception);
            data.put("containerID", containerID);
            data.put("serviceID", serviceID);
            data.put("nodeID", nodeID);

            Notification notification = new Notification("EXCEPTION", exception.getMessage(), data);
            session.persist(notification);

            tx.commit();

            return notification.getId();

        } catch (Exception e){
            tx.rollback();
            e.printStackTrace(System.err);
            return -1;
        }
    }

    public long createExceptionNotification(SessionBuilder sessionBuilder, Throwable exception, String containerID, String serviceID, String nodeID) {
        try (Session session = sessionBuilder.openSession()) {
            return createExceptionNotification(session, exception, containerID, serviceID, nodeID);
        }
    }

    public long createExceptionNotification(SessionBuilder sessionBuilder, Throwable exception, ServiceMetaData metaData) {
        try (Session session = sessionBuilder.openSession()) {
            return createExceptionNotification(session, exception, metaData.getContainerID(), metaData.getServiceID(), metaData.getNodeID());
        }
    }

}
