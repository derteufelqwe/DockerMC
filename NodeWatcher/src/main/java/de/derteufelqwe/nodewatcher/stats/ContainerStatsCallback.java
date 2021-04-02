package de.derteufelqwe.nodewatcher.stats;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Statistics;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.ContainerStats;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.exceptions.ContainerNoLongerExistsException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

import javax.persistence.PersistenceException;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Callback method for the docker stats command. For every stat (1 per second) a DB entry gets created.
 */
public class ContainerStatsCallback implements ResultCallback<Statistics> {

    private final int MAX_FAILS = 5;

    private Logger logger = LogManager.getLogger(getClass().getName());
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();
    private String containerId;
    private int noResultCounter = 0;
    private long sysCpuOld = -1;
    private long cpuCpuOld = -1;


    public ContainerStatsCallback(String containerId) {
        this.containerId = containerId;
    }


    @Override
    public void onStart(Closeable closeable) {
        logger.info("Added container " + this.containerId + ".");
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
                    DBContainer dbContainer = session.getReference(DBContainer.class, containerId);

                    ContainerStats containerStats = new ContainerStats(dbContainer, ts, (float) cpuPercent, ramUsage);
                    session.persist(containerStats);

                } finally {
                    tx.commit();
                }
            }

            noResultCounter = 0;

        // Null if the container is not running anymore
        } catch (NullPointerException e) {
            noResultCounter++;

            if (noResultCounter >= MAX_FAILS) {
                throw new ContainerNoLongerExistsException(this.containerId);
            }

        } catch (Exception e2) {
            logger.error("Caught exception: {}.", e2.getMessage());
            e2.printStackTrace(System.err);
            CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e2, NodeWatcher.getMetaData());
        }

    }

    @Override
    public void onError(Throwable throwable) {
        // This exception is no error and just indicates that the container is stopped.
        if (throwable instanceof ContainerNoLongerExistsException) {
            logger.info(throwable.getMessage());

        } else if (throwable instanceof PersistenceException) {
            Throwable cause = throwable.getCause();
            // Indicates that no DBContainer exists
            if (cause instanceof ConstraintViolationException) {
                logger.error("DBContainer {} not found.", ((ConstraintViolationException) cause).getConstraintName());

            } else {
                logger.error(throwable.getMessage());
            }

        } else if (throwable instanceof NotFoundException) {
            logger.error("Container {} not found on host.", containerId);

        } else {
            logger.error(throwable.getMessage());
            throwable.printStackTrace();
            CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, throwable, NodeWatcher.getMetaData());
        }
    }

    @Override
    public void onComplete() {
        logger.info("Removed container " + this.containerId + ".");
    }

    @Override
    public void close() throws IOException {

    }


    private float toMb(long toConvert) {
        return (float) (Math.round((toConvert / 1024.0 / 1024.0) * 1000) / 1000.0);
    }


}
