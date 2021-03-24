package de.derteufelqwe.driver;

import de.derteufelqwe.commons.hibernate.objects.Log;
import de.derteufelqwe.driver.protobuf.Entry;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Timestamp;

public class LogConsumer implements Runnable {

    private final DatabaseWriter databaseWriter = DMCLogDriver.getDatabaseWriter();

    private String fileName;
    private String container;


    public LogConsumer(String fileName, String container) {
        this.fileName = fileName;
        this.container = container;
    }

    @SneakyThrows
    @Override
    public void run() {
        File file = new File(fileName);
        InputStream is = new FileInputStream(file);

        int overallReadCnt = 0;
        while (overallReadCnt >= 0) {
//            TimeUnit.MILLISECONDS.sleep(100);
            byte[] size = new byte[4];
            overallReadCnt = is.read(size, 0, size.length);

            int msgLength = ByteBuffer.wrap(size).getInt();

            byte[] buffer = new byte[msgLength];
            int readCnt = 0;
            while (readCnt < msgLength) {
                readCnt = is.read(buffer, readCnt, buffer.length);
            }

            Entry.LogEntry logEntry = Entry.LogEntry.parseFrom(buffer);

            Log log = new Log(
                    logEntry.getLine().toStringUtf8(),
                    new Timestamp(logEntry.getTimeNano() / 1_000_000),
                    container
            );

            databaseWriter.pushLog(log);
        }

    }

}
