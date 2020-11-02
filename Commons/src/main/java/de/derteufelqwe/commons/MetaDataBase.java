package de.derteufelqwe.commons;

import de.derteufelqwe.commons.exceptions.ConfigException;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * Gets an ip in the overnet network from the container.
     * @return
     */
    protected String overnetIp() {
        Map<String, String> ips = this.getIpMap();
        List<Integer> adapters = ips.keySet().stream()
                .filter(k -> k.startsWith("eth"))
                .map(k -> k.substring(3))
                .map(Integer::parseInt)
                .sorted()
                .collect(Collectors.toList());

        if (adapters.size() < 2) {
            throw new ConfigException("Container has less than 2 network adapters.");
        }

        int secondLast = adapters.get(adapters.size() - 2);
        String adapterName = "eth" + secondLast;
        String ip = ips.get(adapterName);

        if (!ip.split("\\.")[0].equals(Constants.SUBNET_OVERNET.split("\\.")[0])) {
            throw new ConfigException("Selected adapter doesn't match the required subnet.");
        }

        return ip;
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
