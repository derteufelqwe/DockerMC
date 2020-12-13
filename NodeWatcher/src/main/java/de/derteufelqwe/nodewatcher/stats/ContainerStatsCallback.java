package de.derteufelqwe.nodewatcher.stats;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Statistics;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Container;
import de.derteufelqwe.commons.hibernate.objects.ContainerStats;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.misc.ContainerNoLongerExistsException;
import de.derteufelqwe.nodewatcher.misc.InvalidSystemStateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

public class ContainerStatsCallback implements ResultCallback<Statistics> {

    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();
    private String containerId;
    private Container containerObj; // To map the stats to it
    private int noResultCounter = 0;
    private long sysCpuOld = -1;
    private long cpuCpuOld = -1;


    public ContainerStatsCallback(String containerId) {
        this.containerId = containerId;
    }


    @Override
    public void onStart(Closeable closeable) {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                Container container = session.get(Container.class, this.containerId);
                if (container == null) {
                    throw new InvalidSystemStateException("Failed to find container %s for the ContainerStatsCallback!", this.containerId);
                }
                this.containerObj = container;

            } finally {
                tx.commit();
            }
        }

        System.out.println("[ContainerStats] Added container " + this.containerId + ".");
    }

    @Override
    public void onNext(Statistics object) {
        try {
            // Ram usage
            float ramUsage = this.toMb(object.getMemoryStats().getUsage());

            long sysCpuNew = object.getCpuStats().getSystemCpuUsage();
            long cpuCpuNew = object.getCpuStats().getCpuUsage().getTotalUsage();

            if (sysCpuOld == -1 || cpuCpuOld == -1) {
                this.sysCpuOld = sysCpuNew;
                this.cpuCpuOld = cpuCpuNew;
                return;
            }

            double cpuPercent = 100.0 / (sysCpuNew - this.sysCpuOld) * (cpuCpuNew - this.cpuCpuOld) * object.getCpuStats().getOnlineCpus();

            noResultCounter = 0;
            this.sysCpuOld = sysCpuNew;
            this.cpuCpuOld = cpuCpuNew;


            try (Session session = sessionBuilder.openSession()) {
                Transaction tx = session.beginTransaction();

                try {
                    Timestamp ts = new Timestamp(new Date().getTime());
                    ContainerStats containerStats = new ContainerStats(this.containerObj, ts, (float) cpuPercent, ramUsage);
                    session.persist(containerStats);


                } finally {
                    tx.commit();
                }
            }


        // Null if the container is not running anymore
        } catch (NullPointerException e) {
//            e.printStackTrace();
            noResultCounter++;

            if (noResultCounter >= 6) {
                throw new ContainerNoLongerExistsException("Container %s no longer available.", this.containerId);
            }
        }

    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println(throwable);
    }

    @Override
    public void onComplete() {
        System.out.println("[ContainerStats] Removed container " + this.containerId + ".");
    }

    @Override
    public void close() throws IOException {

    }


    private float toMb(long toConvert) {
        return (float) (Math.round((toConvert / 1024.0 / 1024.0) * 1000) / 1000.0);
    }


}
