package de.derteufelqwe.ServerManager.utils;

import de.derteufelqwe.ServerManager.DBQueries;
import de.derteufelqwe.commons.hibernate.objects.DBServiceHealth;
import org.hibernate.Session;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ServiceHealthAnalyzer {

    private final Session session;
    private final String serviceID;


    public ServiceHealthAnalyzer(Session session, String serviceID) {
        this.session = session;
        this.serviceID = serviceID;
    }


    public List<DBServiceHealth> analyze(long timewindow) {
        List<String> latestErrors = DBQueries.getLatestServiceErrors(session, serviceID, new Timestamp(System.currentTimeMillis() - timewindow));
        List<DBServiceHealth> healths = new ArrayList<>();

        for (String error : latestErrors) {
            DBServiceHealth health = DBQueries.getLatestEntryForServiceHealthError(session, serviceID, error);
            if (health != null) {
                healths.add(health);
            }
        }

        return healths;
    }


}
