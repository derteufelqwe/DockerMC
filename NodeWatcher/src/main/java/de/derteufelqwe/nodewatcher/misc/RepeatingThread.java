package de.derteufelqwe.nodewatcher.misc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A custom version of the thread class, which runs a function repeatedly until it's interrupted
 */
public abstract class RepeatingThread extends Thread {

    private int interval;   // In seconds
    private AtomicBoolean doRun = new AtomicBoolean(true);


    public RepeatingThread(int interval) {
        this.interval = interval;
    }

    public void interrupt() {
        this.doRun.set(false);
    }

    /**
     * A custom sleep function, which checks every second if the program should still run and exits if not
     *
     * @param duration
     */
    private void interpretableSleep(long duration) throws InterruptedException {
        for (long i = 0; i < duration; i++) {
            TimeUnit.SECONDS.sleep(1);

            // Break the sleeping if the whole process gets interrupted
            if (!this.doRun.get()) {
                return;
            }
        }
    }


    @Override
    public void run() {
        while (this.doRun.get()) {
            try {
                this.repeatedRun();

            } finally {
                try {
                    this.interpretableSleep(interval);

                } catch (InterruptedException e) {
                    this.doRun.set(false);
                    this.onInterruptedException(e);
                }
            }
        }
    }

    public abstract void repeatedRun();

    /**
     * Called when an interrupted exception breaks the sleeping process.
     */
    public void onInterruptedException(InterruptedException exception) {

    }

}
