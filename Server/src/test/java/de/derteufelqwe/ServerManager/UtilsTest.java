package de.derteufelqwe.ServerManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    private static List<String> commandList = new ArrayList<>();
    private static List<List<String>> resultList = new ArrayList<>();


    @BeforeAll
    static void setup() {
        commandList.add("Hallo Welt.");
        resultList.add(Arrays.asList("Hallo", "Welt."));

        commandList.add("Hallo \"ich bin\" ein Test .");
        resultList.add(Arrays.asList("Hallo", "ich bin", "ein", "Test", "."));
    }


    @Test
    void splitArgString() {

        for (int i = 0; i < commandList.size(); i++) {
            assertEquals(Utils.splitArgString(commandList.get(i)), resultList.get(i));
        }

    }

}