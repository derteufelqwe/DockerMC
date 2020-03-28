package de.derteufelqwe.ServerManager;

import de.derteufelqwe.ServerManager.utils.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import de.derteufelqwe.commons.Constants;

public class Utils {

    // Regex to split String respecting quotes. Source: https://stackoverflow.com/questions/18893390/splitting-on-comma-outside-quotes
    private static Pattern SPLIT_REGEX = Pattern.compile("\\s(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)");


    public static List<String> splitArgString(String argString) {
        List<String> splits = Arrays.asList(SPLIT_REGEX.split(argString));

        splits = splits.stream()
                .map(e -> e.replace("\"", ""))
                .collect(Collectors.toList());


        return splits;
    }


    /**
     * Creates the labels required for container identification.
     * @return
     */
    public static Map<String, String> quickLabel(Constants.ContainerType containerType) {
        Map<String, String> labelsMap = new HashMap<>();
        labelsMap.put(Constants.DOCKER_IDENTIFIER_KEY, Constants.DOCKER_IDENTIFIER_VALUE);
        labelsMap.put(Constants.CONTAINER_IDENTIFIER_KEY, containerType.name());

        return labelsMap;
    }

}
