package de.derteufelqwe.commons;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.permissions.Permission;
import de.derteufelqwe.commons.hibernate.objects.permissions.PermissionGroup;
import lombok.Getter;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.sql.Timestamp;

/**
 * Adds debug entries to the database
 */
public class DatabaseDebugEntries {

    private final String DEBUG_SERVICE_ID = "debugserviceid";

    private SessionBuilder sessionBuilder = new SessionBuilder("dockermc", "admin", "ubuntu1", Constants.POSTGRESDB_PORT);


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
                        "MINECRAFT_POOL",
                        2
                );

                session.persist(service);
                tx.commit();
                System.out.println("Created debug service.");

            } else {
                System.out.println("Debug service already exists.");
            }

        }
    }

    public void addTestPermissions() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            Permission p1 = new Permission("test.perm1");
            Permission p2 = new Permission("test.perm2", new Timestamp(System.currentTimeMillis() + 100000));
            Permission p3 = new Permission("test.perm3");
            Permission p4 = new Permission("test.perm4");
            Permission p5 = new Permission("test.perm5");
            Permission p6 = new Permission("test.perm6", new Timestamp(System.currentTimeMillis() + 2000));

            PermissionGroup g1 = new PermissionGroup("group1");
            PermissionGroup g2 = new PermissionGroup("group2");
            PermissionGroup g3 = new PermissionGroup("group3");

            g2.setParent(g1);

            p1.setGroup(g1);
            p2.setGroup(g1);

            p3.setGroup(g2);
            p4.setGroup(g2);

            p5.setGroup(g3);
            p6.setGroup(g3);


            session.persist(g1);
            session.persist(g2);
            session.persist(g3);

            session.persist(p1);
            session.persist(p2);
            session.persist(p3);
            session.persist(p4);
            session.persist(p5);
            session.persist(p6);

            tx.commit();
        }
    }


    public static void main(String[] args) {
        DatabaseDebugEntries entries = new DatabaseDebugEntries();

        entries.addDebugService();
//        entries.addTestPermissions();
    }

}
