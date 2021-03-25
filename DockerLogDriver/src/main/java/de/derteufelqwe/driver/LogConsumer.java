package de.derteufelqwe.driver;

import com.google.protobuf.InvalidProtocolBufferException;
import de.derteufelqwe.commons.hibernate.objects.Log;
import de.derteufelqwe.driver.protobuf.Entry;
import lombok.Getter;
import lombok.SneakyThrows;
import org.checkerframework.checker.units.qual.A;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;

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
                System.err.printf("Failed to read file %s. Error: %s.%n", fileName, e.getMessage());
                e.printStackTrace();
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
                System.err.printf("Failed to find file %s.%n", fileName);
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
                    System.err.printf("Failed to parse protobuf message. Error: %s.%n", e.getMessage());
                    continue;
                }

                Log log = new Log(
                        logEntry.getLine().toStringUtf8(),
                        new Timestamp(logEntry.getTimeNano() / 1_000_000),
                        container,
                        parseSource(logEntry.getSource())
                );

                databaseWriter.pushLog(log);

                if (logEntry.hasPartialLogMetadata()) {
                    System.err.println("Found partial log metadata");
                    System.out.println(logEntry);
                }
            }

        } catch (Exception e) {
            System.err.printf("Exception occurred in LogConsumer. %s%n", e.getMessage());
            e.printStackTrace(System.err);
        }

    }


    private Log.Source parseSource(String source) {
        try {
            return Log.Source.valueOf(source.toUpperCase());

        } catch (IllegalArgumentException e) {
            System.err.println("Found unknown log source " + source);
            return Log.Source.UNKNOWN;
        }
    }

}
