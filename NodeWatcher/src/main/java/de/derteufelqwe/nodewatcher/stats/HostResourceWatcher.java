package de.derteufelqwe.nodewatcher.stats;

import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.commons.hibernate.objects.NodeStats;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.annotation.CheckForNull;
import javax.annotation.CheckReturnValue;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Monitors the resource usage of the swarm node host
 */
public class HostResourceWatcher extends Thread {

    private final Pattern RE_MEM_FREE = Pattern.compile("MemAvailable:\\s+(\\d+).+");
    private final Pattern RE_CPU_USAGE = Pattern.compile("cpu\\s+(\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+)");

    private Logger logger = NodeWatcher.getLogger();
    private SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();

    private final String swarmNodeId = NodeWatcher.getSwarmNodeId();
    private boolean doRun = true;
    private Integer maxRam;
    private CpuLoadData oldData;


    public HostResourceWatcher() {
        this.maxRam = this.getHostsMaxRam();
    }

    @SneakyThrows
    @Override
    public void run() {
        this.oldData = getCpuData();

        try (Session session = sessionBuilder.openSession()) {
            while (this.doRun) {
                TimeUnit.SECONDS.sleep(1);

                Transaction tx = session.beginTransaction();
                try {
                    int ramUsage = this.getRamUsage();
                    double cpuUsage = this.getCpuUsagePercent();

                    if (ramUsage < 0) {
                        logger.error("Read invalid host RAM usage.");
                        continue;
                    }

                    if (cpuUsage < 0) {
                        logger.error("Read invalid host CPU usage.");
                        continue;
                    }

                    Node node = session.get(Node.class, swarmNodeId);
                    if (node == null) {
                        logger.error("Failed to get node for id '{}' .", swarmNodeId);
                        continue;
                    }

                    NodeStats nodeStats = new NodeStats(node, new Timestamp(new Date().getTime()), (float) cpuUsage, ramUsage);
                    session.persist(nodeStats);

                } finally {
                    tx.commit();
                }
            }
        }

    }

    public void interrupt() {
        this.doRun = false;
    }


    // -----  Utility methods  -----

    /**
     * Returns the max amount of ram of the local swarm node
     *
     * @return
     */
    @CheckReturnValue
    private Integer getHostsMaxRam() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                Node node = session.get(Node.class, NodeWatcher.getSwarmNodeId());
                if (node == null) {
                    logger.error("Failed to find node {} in database.", swarmNodeId);
                    return -1;
                }

                return node.getMaxRam();

            } finally {
                tx.commit();
            }
        }
    }

    /**
     * Returns the current ram usage of the host.
     * @return
     */
    @CheckReturnValue
    private int getRamUsage() {
        String output = Utils.executeCommandOnHost(new String[]{"cat", "/proc/meminfo"});
        Matcher m = RE_MEM_FREE.matcher(output);

        if (m.find()) {
            try {
                int freeRam = Integer.parseInt(m.group(1));
                return this.maxRam - freeRam;

            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return -1;
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
