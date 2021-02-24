package de.derteufelqwe.bungeeplugin.permissions;

import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PermissionGroupStoreTest {

    private SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", "ubuntu1", Constants.POSTGRESDB_PORT);

    @SneakyThrows
    @Test
    void init() {
        PermissionGroupStore store = new PermissionGroupStore(sessionBuilder);
        store.init();

        long start = System.currentTimeMillis();

        assertTrue(store.hasPermission(59L, "arne.test.a.b"));

//        for (int i = 0; i < 100000; i++) {
//            // Normal permissions
//            assertTrue(store.hasPermission(59L, "test.perm1"));
//            assertTrue(store.hasPermission(59L, "test.perm3"));
//            assertFalse(store.hasPermission(59L, "test.perm5"));
//            assertFalse(store.hasPermission(59L, "test.perm6"));
//
//            // Timed out permission
//            assertFalse(store.hasPermission(59L, "test.perm2"));
//
//            // Star permission
//            assertTrue(store.hasPermission(59L, "arne.test.a.b"));
//            assertTrue(store.hasPermission(59L, "arne.test.a.c"));
//            assertFalse(store.hasPermission(59L, "arne.test.b.c"));
//        }

        long end = System.currentTimeMillis();

        System.out.println("Duration: " + (end - start));
    }
}