package de.derteufelqwe.driver;

import com.google.common.util.concurrent.MoreExecutors;
import lombok.SneakyThrows;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Queue;
import java.util.concurrent.*;

public class ThreadPoolTests {

    @SneakyThrows
    public static void main(String[] args) {
//        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
//        ExecutorService pool;
////        pool = new ThreadPoolExecutor(1, 3, 0, TimeUnit.MINUTES, queue);
////        pool = Executors.newCachedThreadPool();
//        pool = MoreExecutors.getExitingExecutorService((ThreadPoolExecutor) Executors.newCachedThreadPool(), 10, TimeUnit.SECONDS);
//
//        Future<?> f1 = pool.submit(new Task(1));
//
//        pool.submit(new Task(2));
//
//        pool.submit(new Task(3));
//
//        System.out.println("Done");
//        TimeUnit.MILLISECONDS.sleep(100);
//
////        pool.shutdown();
////        pool.shutdownNow();
////        TimeUnit.MINUTES.sleep(10);

        try {
            throw new RuntimeException("fuck off");

        } catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
            e.printStackTrace();
        }


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
