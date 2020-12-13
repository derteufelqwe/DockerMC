package de.derteufelqwe.nodewatcher.logs;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Container;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The callback for docker log downloading, which updates the logs in the database.
 * Also responsible for removing containers from active log download
 */
public class LogLoadCallback implements ResultCallback<Frame> {

    private final Pattern RE_TIMESTAMP = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");

    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();
    private final String containerId;
    private StringBuilder rawLogBuilder = new StringBuilder();
    private ContainerLogFetcher caller; // To be able to remove deactivated containers


    public LogLoadCallback(String containerId, ContainerLogFetcher caller) {
        this.containerId = containerId;
        this.caller = caller;
    }


    @Override
    public void onStart(Closeable closeable) {

    }

    @Override
    public void onNext(Frame object) {
        this.rawLogBuilder.append(new String(object.getPayload()));
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println("Failed to download " + this.containerId + " containers log.");
    }

    @Override
    public void onComplete() {
        // Only run if new logs were found
        if (this.rawLogBuilder.length() == 0) {
            return;
        }

        // Get the new timestamp
        Timestamp lastLogTimestamp = this.getLastTimestamp();
        if (lastLogTimestamp == null) {
            System.err.println("Failed to parse the last log timestamp for " + this.containerId + "!");
            return;
        }

        String log = this.getLogWithoutTimestamps();

        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                Container container = session.get(Container.class, this.containerId);
                if (container == null) {
                    System.err.println("Failed to find container " + this.containerId + "!");
                    return;
                }

                // ToDo: Prevent log line duplication
                // Dont add new logs when the last log timestamps are identical
                if (lastLogTimestamp.equals(container.getLastLogTimestamp())) {
                    return;
                }

                container.appendToLog(log);
                container.setLastLogTimestamp(lastLogTimestamp);

                session.update(container);
                System.out.println("updated logs for " + this.containerId);

                // Remove the container from active log downloading when it's stopped by now
                if (container.getStopTime() != null) {
                    this.caller.removeContainer(this.containerId);
                }

            } finally {
                tx.commit();
            }
        }

    }

    @Override
    public void close() throws IOException {

    }


    /**
     * Parses the latest timestamp from the newly read logs.
     */
    private Timestamp getLastTimestamp() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        Matcher m = RE_TIMESTAMP.matcher(this.rawLogBuilder.toString());
        String timeString = null;
        while (m.find()) {
            timeString = m.group(m.groupCount());
        }

        try {
            return new Timestamp(format.parse(timeString).getTime());

        } catch (ParseException | NullPointerException e) {
            System.err.println("Failed to parse last log timestamp for container " + this.containerId + ".");
            return null;
        }
    }

    /**
     * Returns the read container log without dockers timestamp information
     * @return
     */
    private String getLogWithoutTimestamps() {
        String parsedLog = this.rawLogBuilder.toString().replaceAll("(^|\n).{31}", "\n");

        return parsedLog.substring(1);
    }

}
