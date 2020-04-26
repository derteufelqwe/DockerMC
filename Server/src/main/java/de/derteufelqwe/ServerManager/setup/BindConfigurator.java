package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.ServerManager.ContainerGetter;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.utils.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class to configure the entrys of a Bind9 DNS server
 */
@Deprecated
public class BindConfigurator {

    private boolean autoReload;

    public BindConfigurator(boolean autoReload) {
        this.autoReload = autoReload;
    }

    /**
     * Adds a trailing dot if required
     *
     * @return
     */
    private String addTrailingDot(String input) {
        int len = input.length();
        if (input.substring(len - 2, len - 1) != ".") {
            return input + ".";
        }

        return input;
    }

    /**
     * Formats name and ip to the DNS-config format
     *
     * @return
     */
    private String formatEntry(String name, String ip) {
        return StringUtils.rightPad(name, 30, " ") + StringUtils.rightPad("IN", 6, " ") +
                StringUtils.rightPad("A", 5, " ") + ip + "\n";
    }

    /**
     * Reloads the bind9 server to pick new entries up
     */
    public void reloadBind() {
        String dnsContainerId = new ContainerGetter().getDNSContainer().getId();

        ServerManager.getDocker().execContainer(dnsContainerId, "/etc/init.d/bind9", "reload");
    }

    /**
     * Adds an entry to the DNS config
     *
     * @param entryType System or user entry
     * @param name      DNS name
     * @param ip        IP
     */
    public void addEntry(Type entryType, String name, String ip) {
        String line = this.formatEntry(this.addTrailingDot(name), ip);

        File file = new File(entryType.path);
        if (!file.exists()) {
            throw new FatalDockerMCError("File " + entryType.path + " not found.");
        }

        try {
            FileWriter writer = new FileWriter(file, true);
            writer.write("\n" + line);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (this.autoReload)
            reloadBind();
    }

    /**
     * Removes an Entry from the DNS config
     *
     * @param entryType System or user entry
     * @param name      DNS name
     */
    public boolean removeEntry(Type entryType, String name) {
        boolean hasRemovedSomething = false;
        File file = new File(entryType.path);
        if (!file.exists()) {
            throw new FatalDockerMCError("File " + entryType.path + " not found.");
        }

        try {
            String[] fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8).split("\n");
            List<String> newFileContent = new ArrayList<>();

            for (String line : fileContent) {
                if (!line.startsWith(name)) {
                    newFileContent.add(line);
                } else {
                    hasRemovedSomething = true;
                }
            }

            String newFileString = StringUtils.join(newFileContent, "\n");
            FileUtils.writeStringToFile(file, newFileString);

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (this.autoReload)
            reloadBind();

        return hasRemovedSomething;
    }

    public List<Pair<String, String>> getEntriesAsList(Type entryType) {
        File file = new File(entryType.path);
        if (!file.exists()) {
            throw new FatalDockerMCError("File " + entryType.path + " not found.");
        }

        try {
            List<Pair<String, String>> entries = new ArrayList<>();
            for (String line : FileUtils.readFileToString(file, StandardCharsets.UTF_8).split("\n")) {
                if (line.equals(""))
                    continue;

                String[] lineParts = line.split("\\s+");

                entries.add(new Pair<>(lineParts[0], lineParts[3]));
            }

            return entries;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    public enum Type {
        SYSTEM(Constants.DNS_SYSTEM_ENTRY_PATH),
        USER(Constants.DNS_USER_ENTRY_PATH);

        private String path;

        Type(String path) {
            this.path = path;
        }

        public String path() {
            return path;
        }
    }

}
