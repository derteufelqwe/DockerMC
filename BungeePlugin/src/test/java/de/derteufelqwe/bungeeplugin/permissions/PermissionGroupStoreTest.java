package de.derteufelqwe.bungeeplugin.permissions;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.TestSessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.permissions.Permission;
import de.derteufelqwe.commons.hibernate.objects.permissions.PermissionGroup;
import de.derteufelqwe.commons.hibernate.objects.permissions.PlayerToPermissionGroup;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPermissionStoreTest {

    private final UUID PLAYER1_ID = UUID.randomUUID();
    private final String PLAYER1_NAME = "TestPlayer1";
    private final UUID PLAYER2_ID = UUID.randomUUID();
    private final String PLAYER2_NAME = "TestPlayer2";
    private final UUID PLAYER3_ID = UUID.randomUUID();
    private final String PLAYER3_NAME = "TestPlayer3";

    private final String SERVICE1_ID = UUID.randomUUID().toString();
    private final String SERVICE1_NAME = "Service1";
    private final String SERVICE2_ID = UUID.randomUUID().toString();
    private final String SERVICE2_NAME = "Service2";


    /**
     * Tests the following basic functionalities.
     * - Player permissions work
     * - Group permission work
     * - Inherited permission work
     */
    @Test
    void testBasics() {
        SessionBuilder sessionBuilder = new TestSessionBuilder("test1");
        this.setupBasics(sessionBuilder);

        PlayerPermissionStore store = new PlayerPermissionStore(sessionBuilder);
        store.init();
        store.loadPlayer(PLAYER1_ID);
        store.loadPlayer(PLAYER2_ID);
        store.loadPlayer(PLAYER3_ID);

        // Player1 Permissions
        assertEquals(true,  store.hasPermission(PLAYER1_ID, "test.p.perm1"));
        assertEquals(true,  store.hasPermission(PLAYER1_ID, "test.p.perm2"));
        assertEquals(false, store.hasPermission(PLAYER1_ID, "test.p.perm3"));
        assertEquals(false, store.hasPermission(PLAYER1_ID, "test.perm1"));
        assertEquals(false, store.hasPermission(PLAYER1_ID, "test.perm3"));
        assertEquals(false, store.hasPermission(PLAYER1_ID, "test.perm5"));

        // Player 2 Permissions
        assertEquals(true,  store.hasPermission(PLAYER2_ID, "test.p.perm3"));
        assertEquals(false, store.hasPermission(PLAYER2_ID, "test.p.perm1"));
        assertEquals(true,  store.hasPermission(PLAYER2_ID, "test.perm1"));
        assertEquals(true,  store.hasPermission(PLAYER2_ID, "test.perm2"));

        // Player 3 Permissions
        assertEquals(true,  store.hasPermission(PLAYER3_ID, "test.p.perm4"));
        assertEquals(false, store.hasPermission(PLAYER3_ID, "test.p.perm3"));
        assertEquals(false, store.hasPermission(PLAYER3_ID, "test.p.perm1"));
        assertEquals(false, store.hasPermission(PLAYER3_ID, "test.perm1"));
        assertEquals(true,  store.hasPermission(PLAYER3_ID, "test.perm3"));
        assertEquals(true,  store.hasPermission(PLAYER3_ID, "test.perm4"));
        assertEquals(true,  store.hasPermission(PLAYER3_ID, "test.perm5"));
        assertEquals(true,  store.hasPermission(PLAYER3_ID, "test.perm6"));
    }

    /**
     * Following setup:
     *  Group1:
     *      no parent
     *      test.perm1
     *      test.perm2
     *
     *  Group2:
     *      parent group3
     *      test.perm3
     *      test.perm4
     *
     *  Group3:
     *      no parent
     *      test.perm5
     *      test.perm6
     *
     *  Player1:
     *      No group
     *      test.p.perm1
     *      test.p.perm2
     *
     *  Player2:
     *      group1
     *      test.p.perm3
     *
     *  Player3:
     *      group2
     *      test.p.perm4
     */
    public void setupBasics(SessionBuilder sessionBuilder) {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            PermissionGroup g1 = new PermissionGroup("group1");
            PermissionGroup g2 = new PermissionGroup("group2");
            PermissionGroup g3 = new PermissionGroup("group3");

            g2.setParent(g3);

            session.persist(g1);
            session.persist(g2);
            session.persist(g3);

            // Group 1
            Permission p1 = new Permission("test.perm1");
            p1.setGroup(g1);
            Permission p2 = new Permission("test.perm2");
            p2.setGroup(g1);

            session.persist(p1);
            session.persist(p2);

            // Group 2
            Permission p3 = new Permission("test.perm3");
            p3.setGroup(g2);
            Permission p4 = new Permission("test.perm4");
            p4.setGroup(g2);

            session.persist(p3);
            session.persist(p4);

            // Group 3
            Permission p5 = new Permission("test.perm5");
            p5.setGroup(g3);
            Permission p6 = new Permission("test.perm6");
            p6.setGroup(g3);

            session.persist(p5);
            session.persist(p6);

            // Create the player
            DBPlayer player1 = new DBPlayer(PLAYER1_ID, PLAYER1_NAME);
            DBPlayer player2 = new DBPlayer(PLAYER2_ID, PLAYER2_NAME);
            DBPlayer player3 = new DBPlayer(PLAYER3_ID, PLAYER3_NAME);

            session.persist(player1);
            session.persist(player2);
            session.persist(player3);

            PlayerToPermissionGroup ptpg1 = new PlayerToPermissionGroup(player2, g1);
            PlayerToPermissionGroup ptpg2 = new PlayerToPermissionGroup(player3, g2);

            session.persist(ptpg1);
            session.persist(ptpg2);

            // Setup player 1
            Permission pp1 = new Permission("test.p.perm1");
            pp1.setPlayer(player1);
            Permission pp2 = new Permission("test.p.perm2");
            pp2.setPlayer(player1);

            session.persist(pp1);
            session.persist(pp2);

            // Setup player 2
            Permission pp3 = new Permission("test.p.perm3");
            pp3.setPlayer(player2);

            session.persist(pp3);

            // Setup player 3
            Permission pp4 = new Permission("test.p.perm4");
            pp4.setPlayer(player3);

            session.persist(pp4);

            tx.commit();
        }
    }


    /**
     * Tests if the star permissions get processed properly
     */
    @Test
    void testStarPermissions() {
        SessionBuilder sessionBuilder = new TestSessionBuilder("test2");
        this.setupStarPermissions(sessionBuilder);

        PlayerPermissionStore store = new PlayerPermissionStore(sessionBuilder);
        store.init();
        store.loadPlayer(PLAYER1_ID);
        store.loadPlayer(PLAYER2_ID);
        store.loadPlayer(PLAYER3_ID);

        // Player 1
        assertEquals(true,  store.hasPermission(PLAYER1_ID, "test.g1.*"));
        assertEquals(true,  store.hasPermission(PLAYER1_ID, "test.g1.test1"));
        assertEquals(true,  store.hasPermission(PLAYER1_ID, "test.g1.test1.test2"));
        assertEquals(false, store.hasPermission(PLAYER1_ID, "test.g2"));
        assertEquals(true,  store.hasPermission(PLAYER1_ID, "test.p1"));
        assertEquals(true,  store.hasPermission(PLAYER1_ID, "test.p2.*"));
        assertEquals(true,  store.hasPermission(PLAYER1_ID, "test.p2.test1"));
        assertEquals(true,  store.hasPermission(PLAYER1_ID, "test.p2.test1.test2"));
        assertEquals(false,  store.hasPermission(PLAYER1_ID, "test.p3"));
        assertEquals(false,  store.hasPermission(PLAYER1_ID, "test.p3.test1"));

        // Player 2
        assertEquals(true,  store.hasPermission(PLAYER2_ID, "*"));
        assertEquals(true,  store.hasPermission(PLAYER2_ID, "test.perm1.test1"));
        assertEquals(true,  store.hasPermission(PLAYER2_ID, "mist.perm.1"));
        assertEquals(true,  store.hasPermission(PLAYER2_ID, "test.g1.*"));

        // Player 2
        assertEquals(true,  store.hasPermission(PLAYER3_ID, "*"));
        assertEquals(true,  store.hasPermission(PLAYER3_ID, "test.perm1.test1"));
        assertEquals(true,  store.hasPermission(PLAYER3_ID, "mist.perm.1"));
        assertEquals(true,  store.hasPermission(PLAYER3_ID, "test.g1.*"));
    }

    /**
     * Following setup:
     *  Group1:
     *      test.g1.*
     *
     *  Group2:
     *      *
     *
     *  Player1:
     *      group1
     *      test.p1
     *      test.p2.*
     *
     *  Player2:
     *      group2
     *
     *  Player3
     *      *
     */
    private void setupStarPermissions(SessionBuilder sessionBuilder) {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            // Create groups
            PermissionGroup g1 = new PermissionGroup("group1");
            PermissionGroup g2 = new PermissionGroup("group2");

            session.persist(g1);
            session.persist(g2);

            Permission gp1 = new Permission("test.g1.*");
            gp1.setGroup(g1);
            Permission gp2 = new Permission("*");
            gp2.setGroup(g2);

            session.persist(gp1);
            session.persist(gp2);

            // Create players
            DBPlayer player1 = new DBPlayer(PLAYER1_ID, PLAYER1_NAME);
            DBPlayer player2 = new DBPlayer(PLAYER2_ID, PLAYER2_NAME);
            DBPlayer player3 = new DBPlayer(PLAYER3_ID, PLAYER3_NAME);

            session.persist(player1);
            session.persist(player2);
            session.persist(player3);

            // Add player to the groups
            PlayerToPermissionGroup ptpg1 = new PlayerToPermissionGroup(player1, g1);
            PlayerToPermissionGroup ptpg2 = new PlayerToPermissionGroup(player2, g2);

            session.persist(ptpg1);
            session.persist(ptpg2);

            // Add permission to players
            Permission p1 = new Permission("test.p1");
            p1.setPlayer(player1);
            Permission p2 = new Permission("test.p2.*");
            p2.setPlayer(player1);
            Permission p3 = new Permission("*");
            p3.setPlayer(player3);

            session.persist(p1);
            session.persist(p2);
            session.persist(p3);

            tx.commit();
        }
    }


    /**
     * Tests if service bound permissions work
     */
    @Test
    void testServicePermissions() {
        SessionBuilder sessionBuilder = new TestSessionBuilder("test3");
        this.setupServicePermissions(sessionBuilder);

        PlayerPermissionStore store = new PlayerPermissionStore(sessionBuilder);
        store.init();
        store.loadPlayer(PLAYER1_ID);
        store.loadPlayer(PLAYER2_ID);
        store.loadPlayer(PLAYER3_ID);
    }

    /**
     * Following setup:
     *  Group1:
     *      no parent
     *      test.s1
     *
     *  Group2:
     *      no parent
     *      test.s2 (service 1)
     *
     *  Group3:
     *      group2
     *      test.s3 (service 2)
     *
     *  Player1:
     *      group1
     *      test.p1
     *      test.ps1 (service1)
     *
     *  Player2:
     *      group2
     *      test.ps2 (service1)
     *
     *  Player3:
     *      group 3
     *      test.ps3 (service2)
     */
    private void setupServicePermissions(SessionBuilder sessionBuilder) {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            // Services
            DBService s1 = createService(SERVICE1_ID, SERVICE1_NAME);
            DBService s2 = createService(SERVICE2_ID, SERVICE2_NAME);

            session.persist(s1);
            session.persist(s2);

            // Groups
            PermissionGroup g1 = new PermissionGroup("group1");
            PermissionGroup g2 = new PermissionGroup("group2");
            PermissionGroup g3 = new PermissionGroup("group3");

            session.persist(g1);
            session.persist(g2);
            session.persist(g3);

            // Group permissions
            Permission gp1 = new Permission("test.s1");
            gp1.setGroup(g1);
            Permission gp2 = new Permission("test.s2");
            gp2.setGroup(g2);
            gp2.setService(s1);
            Permission gp3 = new Permission("test.s3");
            gp3.setGroup(g3);
            gp3.setService(s2);

            session.persist(gp1);
            session.persist(gp2);
            session.persist(gp3);

            // Players
            DBPlayer player1 = new DBPlayer(PLAYER1_ID, PLAYER1_NAME);
            DBPlayer player2 = new DBPlayer(PLAYER2_ID, PLAYER2_NAME);
            DBPlayer player3 = new DBPlayer(PLAYER3_ID, PLAYER3_NAME);

            session.persist(player1);
            session.persist(player2);
            session.persist(player3);

            // Player to the groups
            PlayerToPermissionGroup ptpg1 = new PlayerToPermissionGroup(player1, g1);
            PlayerToPermissionGroup ptpg2 = new PlayerToPermissionGroup(player2, g2);
            PlayerToPermissionGroup ptpg3 = new PlayerToPermissionGroup(player3, g3);

            session.persist(ptpg1);
            session.persist(ptpg2);
            session.persist(ptpg3);

            // Players permissions
            Permission p1 = new Permission("test.p1");
            p1.setPlayer(player1);
            Permission p2 = new Permission("test.ps1");
            p2.setPlayer(player1);
            p2.setService(s1);
            Permission p3 = new Permission("test.ps2");
            p3.setPlayer(player2);
            p3.setService(s1);
            Permission p4 = new Permission("test.ps3");
            p4.setPlayer(player3);
            p4.setService(s2);

            session.persist(p1);
            session.persist(p2);
            session.persist(p3);
            session.persist(p4);

            tx.commit();
        }
    }

    private DBService createService(String id, String name) {
        DBService dbService = new DBService();
        dbService.setId(id);
        dbService.setName(name);

        return dbService;
    }

}