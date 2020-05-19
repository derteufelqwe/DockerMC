package de.derteufelqwe.bungeeplugin;

import de.derteufelqwe.bungeeplugin.exceptions.ConfigException;
import lombok.Getter;

/**
 * Contains all the metadata for the container
 */
@Getter
public class MetaData {

    private String containerIP;
    private String taskName;

    public MetaData() {
        this.containerIP = this.getIP("eth0");
        this.taskName = this.getString("TASK_NAME");
    }


    /**
     * Gets a String from the env
     * @param name
     * @return
     */
    private String getString(String name) {
        String data = System.getenv(name);
        if (data == null || data.equals("")) {
            throw new ConfigException("Metadata key %s not found or empty.", name);
        }

        return data;
    }

    /**
     * Gets an int from the env
     * @param name
     * @return
     */
    private int getInt(String name) {
        String data = System.getenv(name);
        if (data == null || data.equals("")) {
            throw new ConfigException("Metadata key %s not found or empty.", name);
        }

        try {
            return Integer.parseInt(data);
        } catch (NumberFormatException e) {
            throw new ConfigException("Metadata key %s (%s) can't be converted to an integer.", name, data);
        }
    }

    /**
     * Gets an ip from the container.
     * @param adapterName
     * @return
     */
    private String getIP(String adapterName) {
        String data = Utils.getIpMap().get(adapterName);

        if (data == null || data.equals("")) {
            throw new ConfigException("Failed to get the container IP.");
        }

        return data;
    }

}
