package de.derteufelqwe.driver;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdDictCompress;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.*;

public class ThreadPoolTests {

    @SneakyThrows
    public static void main(String[] args) {
        
        final String PATH = "C:/Users/Arne/Desktop/Paper 1.16.5/world/region/r.-1.0.mca";

        byte[] raw = FileUtils.readFileToByteArray(new File(PATH));

        byte[] compressed = Zstd.compress(raw, 5);

        double percentage = Math.round(100.0 / raw.length * compressed.length * 100) / 100.0;

        System.out.println("Reduced size by " + (100 - percentage) + "%.");
    }

    public static class Task implements Runnable {

        private int number;

        public Task(int number) {
            this.number = number;
        }

        @SneakyThrows
        @Override
        public void run() {
            try {
                System.out.println("Starting " + number);
                while (true) {
                    System.out.println("Run: " + number);
                    TimeUnit.SECONDS.sleep(3);
                }

            } catch (InterruptedException e) {
                System.err.println("Interrupting");
            }
        }
    }

}
