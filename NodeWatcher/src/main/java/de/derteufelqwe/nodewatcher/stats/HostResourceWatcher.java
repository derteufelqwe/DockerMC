package de.derteufelqwe.nodewatcher.stats;

import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.commons.hibernate.objects.NodeStats;
import de.derteufelqwe.nodewatcher.NodeWatcher;

import de.derteufelqwe.nodewatcher.exceptions.InvalidHostResourcesException;
import de.derteufelqwe.commons.misc.RepeatingThread;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.annotation.CheckForNull;
import java.sql.Timestamp;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Monitors the resource usage of the swarm node host
 */
@Log4j2
public class HostResourceWatcher extends RepeatingThread {

    private final Pattern RE_MEM_FREE = Pattern.compile("MemAvailable:\\s+(\\d+).+");
    private final Pattern RE_CPU_USAGE = Pattern.compile("cpu\\s+(\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+)");

    private SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();

    private final String swarmNodeId = NodeWatcher.getSwarmNodeId();
    private CpuLoadData oldData;
    // Debug
    private final boolean isWindows = Utils.isWindows();


    public HostResourceWatcher() {
        super(1000);
    }

    @Override
    public void run() {
        this.oldData = getCpuData();
        super.run();
    }

    @Override
    public void repeatedRun() {
        sessionBuilder.execute(session -> {
            try {
                Node node = session.get(Node.class, swarmNodeId);
                if (node == null) {
                    log.error("Failed to get node for id '{}' .", swarmNodeId);
                    return;
                }

                int ramUsage = this.getRamUsage(node.getMaxRAM());
                double cpuUsage = this.getCpuUsagePercent();

                NodeStats nodeStats = new NodeStats(node, new Timestamp(new Date().getTime()), (float) cpuUsage, ramUsage);
                session.persist(nodeStats);

            } catch (InvalidHostResourcesException e1) {
                if (!isWindows) {
                    log.error("Reading hosts hardware resources failed with: {}", e1.getMessage());
                }
            }
        });
    }

    @Override
    public void onException(Exception e) {
        super.onException(e);
        log.error("HostResourceWatcher caught exception.", e);
        CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e, NodeWatcher.getMetaData());
    }

    // -----  Utility methods  -----


    /**
     * Returns the current ram usage of the host.
     * @return
     */
    private int getRamUsage(int maxRam) throws InvalidHostResourcesException {
        String output = Utils.executeCommandOnHost(new String[]{"cat", "/proc/meminfo"});
        Matcher m = RE_MEM_FREE.matcher(output);

        if (m.find()) {
            try {
                int freeRam = Integer.parseInt(m.group(1));
                return maxRam - freeRam;

            } catch (NumberFormatException e) {
                throw new InvalidHostResourcesException("Read invalid free RAM value '%s' (no integer).", m.group(1));
            }
        }

        throw new InvalidHostResourcesException("Failed to read meminfo property 'MemAvailable'");
    }

    /**
     * Returns the parsed version of the first line of /proc/stat containing information about the cpu usage
     *
     * @return
     */
    @CheckForNull
    private CpuLoadData getCpuData() {
        String output = Utils.executeCommandOnHost(new String[]{"cat", "/proc/stat"});
        Matcher m = RE_CPU_USAGE.matcher(output);

        if (m.find()) {
            return new CpuLoadData(m);
        }

        return null;
    }

    /**
     * Returns the hosts cpu usage in percent
     *
     * @return
     */
    private double getCpuUsagePercent() {
        CpuLoadData newData = this.getCpuData();

        if (newData == null || oldData == null) {
            return -1.0;
        }

        // Cpu usage calculation
        int totalDelta = newData.sum() - oldData.sum();
        int idleDelta = newData.getAllIdle() - oldData.getAllIdle();

        this.oldData = newData;
        return ((totalDelta - idleDelta) / (double) totalDelta) * 100;
    }


    @AllArgsConstructor
    private static class CpuLoadData {
        public int user;
        public int nice;
        public int system;
        public int idle;
        public int iowait;
        public int irq;
        public int softirq;
        public int steal;
        public int guest;
        public int guest_nice;


        public CpuLoadData(Matcher m) throws RuntimeException {
            try {
                user = Integer.parseInt(m.group(1));
                nice = Integer.parseInt(m.group(2));
                system = Integer.parseInt(m.group(3));
                idle = Integer.parseInt(m.group(4));
                iowait = Integer.parseInt(m.group(5));
                irq = Integer.parseInt(m.group(6));
                softirq = Integer.parseInt(m.group(7));
                steal = Integer.parseInt(m.group(8));
                guest = Integer.parseInt(m.group(9));
                guest_nice = Integer.parseInt(m.group(10));

            } catch (NumberFormatException e) {
                throw new RuntimeException("Failed to parse cpu load data.");
            }
        }


        public int sum() {
            return user + nice + system + idle + iowait + irq + softirq + steal + guest + guest_nice;
        }

        public int getAllIdle() {
            return idle + iowait;
        }

        public int getAllNonIdle() {
            return user + nice + system + irq + softirq + steal + guest + guest_nice;
        }

    }

}
