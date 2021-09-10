package de.derteufelqwe.ServerManager.setup.servers;

import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.templates.ServiceTemplate;
import lombok.SneakyThrows;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

class ServerPoolTest {

    @SneakyThrows
    @Test
    public void testClone() {
        BungeePool p1 = new BungeePool("A", "B", "C", 1.2F, 1, new ServiceConstraints(2), 2);
        BungeePool p2 = (BungeePool) p1.clone();

        assertEquals(p1, p2);

        Field field = ServiceTemplate.class.getDeclaredField("constraints");
        field.setAccessible(true);

        ServiceConstraints constraints1 = (ServiceConstraints) field.get(p1);
        ServiceConstraints constraints2 = (ServiceConstraints) field.get(p2);
        constraints1.getNameConstraints().add("Name");

        assertNotEquals(constraints1, constraints2);
    }

}