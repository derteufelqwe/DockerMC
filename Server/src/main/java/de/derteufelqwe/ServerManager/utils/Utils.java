package de.derteufelqwe.ServerManager.utils;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
     * Converts a String like 512M to the amount of bytes.
     */
    public static long convertMemoryString(String memoryString) {
        String memString = memoryString.toUpperCase();
        int len = memString.length();
        String memChar = memString.substring(len - 1, len);
        int memVal = Integer.parseInt(memString.substring(0, len - 1));

        switch (memChar) {
            case "B":
                return (long) (memVal * Math.pow(1024, 0));
            case "K":
                return (long) (memVal * Math.pow(1024, 1));
            case "M":
                return (long) (memVal * Math.pow(1024, 2));
            case "G":
                return (long) (memVal * Math.pow(1024, 3));

            default:
                throw new FatalDockerMCError("The memory config " + memoryString + " is unknown.");
        }
    }

    public static List<String> getHostIPAddresses() {
        List<String> resList = new ArrayList<>();

        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress i = (InetAddress) ee.nextElement();
                    resList.add(i.getHostAddress());
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }

        return resList;
    }

    public static boolean sleep(TimeUnit timeUnit, long duration) {
        try {
            timeUnit.sleep(duration);
            return true;

        } catch (InterruptedException e) {
            return false;
        }
    }

    public static String getLocalSwarmNode(Docker docker) {
        return docker.getDocker().infoCmd().exec().getSwarm().getNodeID();
    }

    /**
     * Checks if a list of booleans are all false.
     */
    public static boolean allFalse(Boolean... values) {
        for (boolean val : values) {
            if (val)
                return false;
        }

        return true;
    }

    /**
     * Checks if a list of booleans are all true.
     */
    public static boolean allTrue(Boolean... values) {
        for (boolean val : values) {
            if (!val)
                return false;
        }

        return true;
    }

}
