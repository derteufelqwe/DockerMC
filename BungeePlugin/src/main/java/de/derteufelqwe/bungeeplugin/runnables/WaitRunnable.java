package de.derteufelqwe.bungeeplugin.runnables;

import lombok.Getter;

import javax.annotation.CheckForNull;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A custom Runnable, where you can wait for its result.
 * Use this when you want to run multiple tasks in parallel but want to process their results afterwards.
 * @param <T> The return type of your task. Use {@link Void} and return null when you have nothing to return.
 */
public abstract class WaitRunnable<T> implements Runnable {

    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * The result of the exec function. Is null if the method hasn't exited yet or has nothing to return.
     */
    @Getter @CheckForNull
    private T result = null;

    @Getter
    private boolean done = false;

    @Override
    public void run() {
        try {
            this.result = this.exec();

        } finally {
            this.done = true;
            this.latch.countDown();
        }
    }

    /**
     * Your custom code to execute.
     */
    public abstract T exec();

    /**
     * Wait up to duration Milliseconds for the task to finish
     * @param duration Max wait duration in ms
     * @return true = finished, false = unfinished but 2 seconds exceeded
     */
    public boolean awaitCompletion(long duration) {
        try {
            return this.latch.await(duration, TimeUnit.MILLISECONDS);

        } catch (InterruptedException e) {
            return false;
        }
    }

}
