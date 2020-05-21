package de.derteufelqwe.commons;

import de.derteufelqwe.commons.exceptions.ConfigException;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for metadata in containers.
 * The metadata must be passed via environment variables.
 */
public class MetaDataBase {

    public MetaDataBase() {
    }

    /**
     * Gets a String from the env
     * @param name
     * @return
     */
    protected String getString(String name) {
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
    protected int getInt(String name) {
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
    protected String getIP(String adapterName) {
        String data = this.getIpMap().get(adapterName);

        if (data == null || data.equals("")) {
            throw new ConfigException("Failed to get the container IP.");
        }

        return data;
    }

    /**
     * Returns a map with the interface name -> ip
     * @return
     */
    private Map<String, String> getIpMap() {
        Map<String, String> resMap = new HashMap<>();

        Enumeration<NetworkInterface> ifaces = null;
        try {
            ifaces = NetworkInterface.getNetworkInterfaces();

            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                Enumeration<InetAddress> addresses = iface.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        resMap.put(iface.getName(), addr.getHostAddress());
                    }
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }

        return resMap;
    }

}
