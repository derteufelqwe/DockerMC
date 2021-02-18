package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import lombok.Getter;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Adds debug entries to the database
 */
public class DatabaseDebugEntries {

    private final String DEBUG_SERVICE_ID = "debugserviceid";

    private SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", "ubuntu1", Constants.POSTGRESDB_PORT);


    public void addDebugService() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            DBService service = session.get(DBService.class, DEBUG_SERVICE_ID);
            if (service == null) {
                service = new DBService(
                        DEBUG_SERVICE_ID,
                        "DebugService",
                        1024,
                        2.0f,
                        "MINECRAFT_POOL"
                );

                session.persist(service);
                tx.commit();
                System.out.println("Created debug service.");

            } else {
                System.out.println("Debug service already exists.");
            }

        }
    }


    public static void main(String[] args) {
        DatabaseDebugEntries entries = new DatabaseDebugEntries();

        entries.addDebugService();
    }

}
