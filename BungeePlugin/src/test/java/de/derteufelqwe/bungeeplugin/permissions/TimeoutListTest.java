package de.derteufelqwe.bungeeplugin.permissions;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TimeoutListTest {

    @SneakyThrows
    @Test
    void size() {
        TimeoutList<String> list = new TimeoutList<>();
        list.add("Arne", System.currentTimeMillis() + 500);

        assertEquals(1, list.size());
        assertEquals("Arne", list.get(0));

        TimeUnit.SECONDS.sleep(1);

        assertEquals(0, list.size());
    }
}