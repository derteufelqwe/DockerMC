package de.derteufelqwe.minecraftplugin;

import de.derteufelqwe.commons.misc.Pair;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static Map<String, String> getIpMap() {
        Map<String, String> resMap = new HashMap<>();

        Enumeration<NetworkInterface> ifaces = null;
        try {
            ifaces = NetworkInterface.getNetworkInterfaces();

            while( ifaces.hasMoreElements() ) {
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

    /**
     * Returns the list slice indices when displaying long lists on multiple pages in minecraft.
     * @param pageNumber Page number
     * @param itemsPerPage Amount of items per page
     * @param maxLength Length of the available objects to display
     * @return
     */
    public static Pair<Integer, Integer> getPageSlices(int pageNumber, int itemsPerPage, int maxLength) {
        int start = 0;
        int end = itemsPerPage;

        if (pageNumber > 1) {
            start = (pageNumber - 1) * itemsPerPage;
            end = pageNumber * itemsPerPage;
        }

        if (end >= maxLength) {
            end = maxLength;
            start = end / itemsPerPage;
        }

        return new Pair<>(start, end);
    }

}
