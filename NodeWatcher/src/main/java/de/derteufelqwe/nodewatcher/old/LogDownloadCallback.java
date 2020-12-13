package de.derteufelqwe.nodewatcher.old;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import javax.annotation.CheckForNull;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Callback class for downloading the log of a stopped container.
 * This class saves the extracted data to a database.
 */
@NoArgsConstructor
public class LogDownloadCallback implements ResultCallback<Frame> {

    private Pattern RE_TIMESTAMP = Pattern.compile("\\n(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");

    private String rawLog = "";
    private CountDownLatch latch = new CountDownLatch(1);   // Used to identify if the log download is complete

    /**
     * Blocks until the log download is done.
     */
    @SneakyThrows
    public void await() {
        this.latch.await();
    }


    public String getLogMessage() {
        String s = this.rawLog.replaceAll("(^|\n).{31}", "\n");
        return s.substring(1);
    }


    /**
     * Extracts the last log timestamp for the logs
     * @return
     */
    @SneakyThrows
    @CheckForNull
    public Timestamp getLastTimestamp() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        Matcher m = RE_TIMESTAMP.matcher(this.rawLog);
        String timeString = null;
        while (m.find()) {
            timeString = m.group();
        }

        if (timeString == null) {
            return null;
        }

        return new Timestamp(format.parse(timeString.substring(1)).getTime());
    }


    @Override
    public void onStart(Closeable closeable) {

    }

    @Override
    public void onNext(Frame object) {
        String s = new String(object.getPayload());

        this.rawLog += s;
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println(throwable);
    }

    @Override
    public void onComplete() {
        this.latch.countDown();
    }

    @Override
    public void close() throws IOException {

    }
}
