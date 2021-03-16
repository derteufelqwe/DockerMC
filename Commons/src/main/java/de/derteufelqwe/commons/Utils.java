package de.derteufelqwe.commons;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
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

    /**
     *
     * @return
     */
    public static Timestamp getLocalTimestampWithoutTimezone() {
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        ZoneOffset offset = zonedDateTime.getOffset();

        return new Timestamp(System.currentTimeMillis() - (offset.getTotalSeconds() * 1000L));
    }

    public static String formatTimestamp(Timestamp timestamp) {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yy HH:mm:ss");

        return format.format(timestamp);
    }

    public static String formatDuration(long duration) {
        Duration d = Duration.of(duration, ChronoUnit.SECONDS);

        long tmp = d.getSeconds();
        long seconds = tmp % 60;
        tmp = tmp / 60;
        long minutes = tmp % 60;
        tmp = tmp / 60;
        long hours = tmp % 24;
        tmp = tmp / 60;
        long days = tmp;

        if (days == 0)
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        else
            return String.format("%d days %d:%02d:%02d", days, hours, minutes, seconds);
    }

    /**
     * Parses timestamps like "1.12.21-23:10"
     */
    public static Timestamp parseTimestamp(String timestamp) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy-HH:mm");

        return new Timestamp(format.parse(timestamp).getTime());
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

}
