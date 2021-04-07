package de.derteufelqwe.ServerManager.utils;

import de.derteufelqwe.commons.hibernate.objects.DBServiceHealth;
import org.hibernate.Session;

import javax.annotation.CheckForNull;
import javax.persistence.NoResultException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ServiceHealthAnalyzer {

    private Session session;
    private String serviceID;

    public ServiceHealthAnalyzer(Session session, String serviceID) {
        this.session = session;
        this.serviceID = serviceID;
    }


    public List<DBServiceHealth> analyze(long timewindow) {
        List<String> latestErrors = this.getLatestErrors(new Timestamp(System.currentTimeMillis() - timewindow));
        List<DBServiceHealth> healths = new ArrayList<>();

        for (String error : latestErrors) {
            DBServiceHealth health = this.getLatestEntryForError(error);
            if (health != null) {
                healths.add(health);
            }
        }

        return healths;
    }

    @SuppressWarnings("unchecked")
    private List<String> getLatestErrors(Timestamp latestTimestamp) {
        return (List<String>) session.createQuery(
                "SELECT DISTINCT sh.error from DBServiceHealth AS sh WHERE sh.service.id=:sid AND sh.taskState!=:tstate AND sh.createdTimestamp >= :ts"
        )
                .setParameter("sid", serviceID)
                .setParameter("tstate", DBServiceHealth.TaskState.RUNNING)
                .setParameter("ts", latestTimestamp)
                .getResultList();
    }

    @CheckForNull
    private DBServiceHealth getLatestEntryForError(String error) {
        try {
            return session.createQuery(
                    "SELECT sh from DBServiceHealth AS sh WHERE sh.service.id=:sid AND sh.taskState not in :tstates AND sh.error = :error ORDER BY sh.createdTimestamp DESC",
                    DBServiceHealth.class)
                    .setParameter("sid", serviceID)
                    .setParameterList("tstates", new DBServiceHealth.TaskState[]{DBServiceHealth.TaskState.RUNNING, DBServiceHealth.TaskState.STARTING, DBServiceHealth.TaskState.SHUTDOWN})
                    .setParameter("error", error)
                    .setMaxResults(1)
                    .getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }

}
