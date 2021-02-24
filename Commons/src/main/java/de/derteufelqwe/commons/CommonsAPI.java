package de.derteufelqwe.commons;


import com.sun.istack.NotNull;
import de.derteufelqwe.commons.exceptions.DmcAPIException;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.Notification;
import de.derteufelqwe.commons.hibernate.objects.economy.ServiceBalance;
import de.derteufelqwe.commons.hibernate.objects.permissions.PermissionGroup;
import de.derteufelqwe.commons.hibernate.objects.permissions.PlayerToPermissionGroup;
import de.derteufelqwe.commons.hibernate.objects.permissions.ServicePermission;
import de.derteufelqwe.commons.hibernate.objects.permissions.TimedPermission;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;

import javax.annotation.CheckForNull;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @CheckForNull
    @Deprecated
    public TimedPermission getTimedPermissionForPlayer(Session session, DBPlayer player, String permission) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<TimedPermission> cq = cb.createQuery(TimedPermission.class);
        Root<TimedPermission> root = cq.from(TimedPermission.class);

        cq.select(root).where(
                cb.equal(root.get("permissionText"), permission),
                cb.equal(root.join("player").get("uuid"), player.getUuid())
        );

        TypedQuery<TimedPermission> queryRes = session.createQuery(cq);

        try {
            return queryRes.getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }

    @CheckForNull
    @Deprecated
    public ServicePermission getServicePermissionForPlayer(Session session, DBPlayer player, DBService service, String permission) {
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ServicePermission> cq = cb.createQuery(ServicePermission.class);
        Root<ServicePermission> root = cq.from(ServicePermission.class);

        cq.select(root).where(
                cb.equal(root.get("permissionText"), permission),
                cb.equal(root.join("player").get("uuid"), player.getUuid()),
                cb.equal(root.join("service").get("id"), service.getId())
        );

        TypedQuery<ServicePermission> queryRes = session.createQuery(cq);

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



}
