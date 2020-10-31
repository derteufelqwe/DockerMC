package de.derteufelqwe.ServerManager;

import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceConstraintsTest {

    @Test
    public void testClone() {
        ServiceConstraints c1 = new ServiceConstraints();
        ServiceConstraints c2 = c1.clone();
        c1.getNameConstraints().add("Name");

        assertNotEquals(c1, c2);
    }

}