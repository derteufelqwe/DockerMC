package de.derteufelqwe.driver.workers;

import com.google.protobuf.InvalidProtocolBufferException;
import de.derteufelqwe.commons.hibernate.objects.Log;
import de.derteufelqwe.driver.DMCLogDriver;
import de.derteufelqwe.driver.exceptions.StreamClosedException;
import de.derteufelqwe.driver.protobuf.Entry;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class LogConsumer implements Runnable {

    private final Pattern RE_EXCEPTION = Pattern.compile("^([\\w|\\d|\\.|\\_]+(Exception|Error)): ([^\\n]+)");
    private final Pattern RE_EXCEPTION_CAUSED = Pattern.compile("^Caused by: ([\\w|\\d|\\.|\\_]+(Exception|Error)): ([^\\n]+)");
    private final Pattern RE_STACKTRACE = Pattern.compile("^\\s+(at .+|\\.\\.\\. \\d+ more)");

    private final DatabaseWriter databaseWriter = DMCLogDriver.getDatabaseWriter();

    private final String fileName;
    private final String container;

    /**
     * The timestamp, when the last data from the FIFO stream was read
     */
    @Getter
    private AtomicLong lastLogReadTime = new AtomicLong(0);

    private Log rootExceptionLog = null;
    private Log exceptionLog = null;
    private long lastLogTime = 0;


    public LogConsumer(String fileName, String container) {
        this.fileName = fileName;
        this.container = container;
    }


    @Override
    public void run() {
        try {
            File file = new File(fileName);
            InputStream is = new FileInputStream(file);

            while (true) {
                try {
                    int msgLength = this.readSize(is);
                    Entry.LogEntry logEntry = this.readLogMessage(is, msgLength);
                    String message = logEntry.getLine().toStringUtf8();

                    if (logEntry.hasPartialLogMetadata()) {
                        log.error("Found partial log metadata");
                        log.error(logEntry);
                    }

                    Log dbLog = new Log(
                            message,
                            this.getPatchedLogTimestamp(logEntry.getTimeNano(), message),
                            container,
                            parseSource(logEntry.getSource())
                    );

                    if (this.parseForExceptions(dbLog, message)) {
                        continue;
                    }

                    // Normal log message, no exception
                    if (exceptionLog == null) {
                        databaseWriter.pushLog(dbLog);
                    }

                } catch (StreamClosedException e1) {
                    break;

                } catch (InvalidProtocolBufferException e2) {
                    log.error("Failed to parse protobuf message. Error: {}.", e2.getMessage());

                }
            }

        } catch (FileNotFoundException e) {
            log.error("Failed to find file {}. Skipping it.", fileName);

        } catch (Exception e) {
            log.error("Exception occurred in LogConsumer.", e);
        }

    }


    /**
     * Reads from an input stream until a buffer is full
     *
     * @param is
     * @param buffer
     * @return true = Could read required data, false = failed to read data
     */
    private boolean fillBuffer(InputStream is, byte[] buffer) {
        int readCnt = 0;

        while (readCnt >= 0 && readCnt < buffer.length) {
            try {
                readCnt = is.read(buffer, readCnt, buffer.length - readCnt);
                this.lastLogReadTime.set(System.currentTimeMillis());

            } catch (IOException e) {
                log.error("Failed to read file {}.", fileName, e);
                break;
            }
        }

        return readCnt >= 0;
    }

    /**
     * Reads the size of the next message from the stream
     * @param is
     * @return
     * @throws StreamClosedException
     */
    private int readSize(InputStream is) throws StreamClosedException {
        byte[] size = new byte[4];
        if (!fillBuffer(is, size)) {
            throw new StreamClosedException(fileName);
        }

        return ByteBuffer.wrap(size).getInt();
    }

    /**
     * Reads and decodes a log line from the stream
     * @param is
     * @param msgLength
     * @return
     * @throws StreamClosedException
     * @throws InvalidProtocolBufferException
     */
    private Entry.LogEntry readLogMessage(InputStream is, int msgLength) throws StreamClosedException, InvalidProtocolBufferException {
        byte[] buffer = new byte[msgLength];
        if (!fillBuffer(is, buffer)) {
            throw new StreamClosedException(fileName);
        }

        return Entry.LogEntry.parseFrom(buffer);
    }

    /**
     * Parses the logs source (STDOUT / STDERR) with error handling
     * @param source
     * @return
     */
    private Log.Source parseSource(String source) {
        try {
            return Log.Source.valueOf(source.toUpperCase());

        } catch (IllegalArgumentException e) {
            log.error("Found unknown log source {}.", source);
            return Log.Source.UNKNOWN;
        }
    }

    /**
     * Sometimes logs are pushed so fast that the timestamp will be equal on consecutive log messages in postgres
     * (as postgres only supports microseconds instead of nanoseconds).
     * This method catches this problem and increases the new log timestamp to be one microsecond later than the previous one.
     * @param logTime
     * @return
     */
    private Timestamp getPatchedLogTimestamp(long logTime, String message) {
        long diff = lastLogTime - logTime;
        lastLogTime = logTime;

        // The log timestamp difference is less than 1 microsecond
        if (diff >= -1000) {
            lastLogTime += diff + 1000;
        }

        Timestamp timestamp = new Timestamp(lastLogTime / 1_000_000);
        timestamp.setNanos((int) (lastLogTime % 1_000_000_000));

        return timestamp;
    }

    /**
     * Tries to extract exception information from the logs
     * @param dbLog
     * @param message
     * @return true if data was found and it should NOT be submitted to the DB atm.
     */
    private boolean parseForExceptions(Log dbLog, String message) {
        try {
            // Find the start of an exception
            Matcher mException = RE_EXCEPTION.matcher(message);
            if (mException.matches()) {  // Exception beginning
                dbLog.setType(Log.MsgType.EXCEPTION);
                dbLog.setExceptionType(mException.group(1));
                dbLog.setExceptionMessage(mException.group(3));
                rootExceptionLog = dbLog;
                exceptionLog = dbLog;
                return true;
            }

            // Find the corresponding stacktraces
            if (rootExceptionLog != null) {
                Matcher mStacktrace = RE_STACKTRACE.matcher(message);
                if (mStacktrace.matches()) {
                    dbLog.setType(Log.MsgType.STACKTRACE);
                    exceptionLog.getStacktrace().add(dbLog);
                    return true;
                }
            }

            // Find caused by if required
            if (rootExceptionLog != null) {
                Matcher mCaused = RE_EXCEPTION_CAUSED.matcher(message);
                if (mCaused.matches()) {
                    dbLog.setType(Log.MsgType.EXCEPTION);
                    dbLog.setExceptionType(mCaused.group(1));
                    dbLog.setExceptionMessage(mCaused.group(3));
                    exceptionLog.setCausedBy(dbLog);
                    exceptionLog = dbLog;
                    return true;
                }
            }

            // End of stacktrace. Flush the data to the database
            if (rootExceptionLog != null) {
                databaseWriter.pushException(rootExceptionLog);
                exceptionLog = null;
                rootExceptionLog = null;
            }

        } catch (IllegalStateException e) {
            log.error("Parsing log for exceptions threw an error.", e);
            databaseWriter.pushException(rootExceptionLog);
            exceptionLog = null;
            rootExceptionLog = null;
        }

        return false;
    }

}
