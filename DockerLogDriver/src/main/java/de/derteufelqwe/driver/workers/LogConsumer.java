package de.derteufelqwe.driver.workers;

import com.google.protobuf.InvalidProtocolBufferException;
import de.derteufelqwe.commons.hibernate.objects.Log;
import de.derteufelqwe.driver.DMCLogDriver;
import de.derteufelqwe.driver.protobuf.Entry;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.common.aliasing.qual.LeakedToResult;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;

@Log4j2
public class LogConsumer implements Runnable {

    private final DatabaseWriter databaseWriter = DMCLogDriver.getDatabaseWriter();

    private String fileName;
    private String container;

    /**
     * The timestamp, when the last data from the FIFO stream was read
     */
    @Getter
    private AtomicLong lastLogReadTime = new AtomicLong(0);


    public LogConsumer(String fileName, String container) {
        this.fileName = fileName;
        this.container = container;
    }


    /**
     * Reads from an input stream until a buffer is full
     *
     * @param is
     * @param buffer
     * @return true = Could read required data, false = failed to read data
     */
    @SneakyThrows
    private boolean fillBuffer(InputStream is, byte[] buffer) {
        int readCnt = 0;

        while (readCnt >= 0 && readCnt < buffer.length) {
            try {
                readCnt = is.read(buffer, readCnt, buffer.length - readCnt);
                this.lastLogReadTime.set(System.currentTimeMillis());

            } catch (IOException e) {
                log.error("Failed to read file {}. Error: {}.", fileName, e.getMessage());
                log.error(e);
                break;
            }
        }

        return readCnt >= 0;
    }


    @SneakyThrows
    @Override
    public void run() {
        try {
            File file = new File(fileName);
            InputStream is;

            try {
                is = new FileInputStream(file);

            } catch (FileNotFoundException e) {
                log.error("Failed to find file {}.", fileName);
                return;
            }

            while (true) {
                // Read the size of the next message
                byte[] size = new byte[4];
                if (!fillBuffer(is, size)) {
                    break;
                }

                // Read the full message
                int msgLength = ByteBuffer.wrap(size).getInt();
                byte[] buffer = new byte[msgLength];
                if (!fillBuffer(is, buffer)) {
                    break;
                }

                // Decode the message
                Entry.LogEntry logEntry;
                try {
                    logEntry = Entry.LogEntry.parseFrom(buffer);

                } catch (InvalidProtocolBufferException e) {
                    log.error("Failed to parse protobuf message. Error: {}.", e.getMessage());
                    continue;
                }

                Log dbLog = new Log(
                        logEntry.getLine().toStringUtf8(),
                        new Timestamp(logEntry.getTimeNano() / 1_000_000),
                        container,
                        parseSource(logEntry.getSource())
                );

                databaseWriter.pushLog(dbLog);

                if (logEntry.hasPartialLogMetadata()) {
                    log.error("Found partial log metadata");
                    log.error(logEntry);
                }
            }

        } catch (Exception e) {
            log.error("Exception occurred in LogConsumer. {}", e.getMessage());
            log.error(e);
        }

    }


    private Log.Source parseSource(String source) {
        try {
            return Log.Source.valueOf(source.toUpperCase());

        } catch (IllegalArgumentException e) {
            log.error("Found unknown log source {}.", source);
            return Log.Source.UNKNOWN;
        }
    }

}
