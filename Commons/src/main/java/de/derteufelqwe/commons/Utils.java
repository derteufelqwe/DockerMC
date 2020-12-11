package de.derteufelqwe.commons;

import javax.annotation.CheckForNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Utils {

    /**
     * Executes a command on the java host and returns its result
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

}
