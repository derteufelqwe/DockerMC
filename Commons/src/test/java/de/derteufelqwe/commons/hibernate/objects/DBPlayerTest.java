package de.derteufelqwe.commons.hibernate.objects;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.permissions.Permission;
import de.derteufelqwe.commons.hibernate.objects.permissions.PermissionGroup;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DBPlayerTest {

    private final UUID player1Id = UUID.randomUUID();
    private final UUID player2Id = UUID.randomUUID();
    private final UUID player3Id = UUID.randomUUID();
    private final UUID player4Id = UUID.randomUUID();

    private TestSessionBuilder sessionBuilder;


    @BeforeAll
    public void setup() {
        this.sessionBuilder = new TestSessionBuilder("admin", "password", "ubuntu1", 5432);

        this.createPlayers();
        this.createPermissions();
        this.createPermissionGroups();
        this.mapPermissionGroups();
    }

    private void createPlayers() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            DBPlayer player1 = new DBPlayer(player1Id, "Player1");
            DBPlayer player2 = new DBPlayer(player2Id, "Player2");
            DBPlayer player3 = new DBPlayer(player3Id, "Player3");
            DBPlayer player4 = new DBPlayer(player4Id, "Player4");

            session.persist(player1);
            session.persist(player2);
            session.persist(player3);
            session.persist(player4);

            tx.commit();
            System.out.println("[Setup] Created players.");
        }
    }

    private void createPermissionGroups() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            PermissionGroup group1 = new PermissionGroup("group1");
            PermissionGroup group2 = new PermissionGroup("group2");

            session.persist(group1);
            session.persist(group2);

            tx.commit();
            System.out.println("[Setup] Created permission groups.");
        }
    }

    private void createPermissions() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            Permission perm1 = new Permission("perm1");
            Permission perm2 = new Permission("perm2");
            Permission perm3 = new Permission("perm3");
            Permission perm4 = new Permission("perm4");

            session.persist(perm1);
            session.persist(perm2);
            session.persist(perm3);
            session.persist(perm4);

            tx.commit();
            System.out.println("[Setup] Created permissions.");
        }
    }

    private void mapPermissionGroups() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            PermissionGroup group1 = session.get(PermissionGroup.class, 1L);
            PermissionGroup group2 = session.get(PermissionGroup.class, 2L);

            Permission perm1 = session.get(Permission.class, 1L);
            Permission perm2 = session.get(Permission.class, 2L);
            Permission perm3 = session.get(Permission.class, 3L);
            Permission perm4 = session.get(Permission.class, 4L);

            group1.getPermissions().add(perm1);
            group1.getPermissions().add(perm2);

            group2.getPermissions().add(perm3);
            group2.getPermissions().add(perm4);

            session.persist(group1);
            session.persist(group2);

            tx.commit();
            System.out.println("[Setup] Mapped permissions to group..");
        }
    }

    @Test
    public void testPlayers() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            DBPlayer player1 = session.get(DBPlayer.class, player1Id);
            assertEquals("Player1", player1.getName());
            DBPlayer player2 = session.get(DBPlayer.class, player2Id);
            assertEquals("Player2", player2.getName());
            DBPlayer player3 = session.get(DBPlayer.class, player3Id);
            assertEquals("Player3", player3.getName());
            DBPlayer player4 = session.get(DBPlayer.class, player4Id);
            assertEquals("Player4", player4.getName());

            tx.commit();
        }
    }

    @Test
    public void testPermissionGroups() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            PermissionGroup g1 = session.get(PermissionGroup.class, 1L);
            PermissionGroup g2 = session.get(PermissionGroup.class, 2L);

            assertEquals("group1", g1.getName());
            assertEquals("group2", g2.getName());

            List<Long> group1Perms = Arrays.asList(1L, 2L);
            List<Long> group2Perms = Arrays.asList(3L, 4L);

            for (Permission p : g1.getPermissions()) {
                assertTrue(group1Perms.contains(p.getId()));
            }

            for (Permission p : g2.getPermissions()) {
                assertTrue(group2Perms.contains(p.getId()));
            }

            tx.commit();
        }
    }

    @Test
    public void testTest() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            session.get(DBPlayer.class, player1Id);

            tx.commit();
        }
    }


}