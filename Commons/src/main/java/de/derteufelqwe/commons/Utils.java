package de.derteufelqwe.commons;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    /**
     * Executes a command on the java host and returns its result
     *
     * @param commands
     * @return
     */
    public static String executeCommandOnHost(String[] commands) {
        Runtime runtim = Runtime.getRuntime();
        try {
            Process process = runtim.exec(commands);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            StringBuilder output = new StringBuilder();
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                output.append(s);
                output.append("\n");
            }

            return output.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Creates the labels required for container identification.
     *
     * @return
     */
    public static Map<String, String> quickLabel(Constants.ContainerType containerType) {
        Map<String, String> labelsMap = new HashMap<>();
        labelsMap.put(Constants.DOCKER_IDENTIFIER_KEY, Constants.DOCKER_IDENTIFIER_VALUE);
        labelsMap.put(Constants.CONTAINER_IDENTIFIER_KEY, containerType.name());

        return labelsMap;
    }
}
