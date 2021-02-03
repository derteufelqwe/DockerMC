package de.derteufelqwe.commons.misc;

import lombok.Getter;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A map, which values disappear after a certain time
 * @param <K>
 * @param <V>
 */
public class TimeoutMap<K, V> extends Thread {

    private final Map<K, TMValue<V>> data = new ConcurrentHashMap<>();

    /**
     * In ms
     */
    private final int clearInterval;

    private long lastClearTime = 0;

    private boolean doRun = true;


    public TimeoutMap(int clearInterval) {
        this.clearInterval = clearInterval;
    }

    @Override
    public void run() {
        while (this.doRun) {
            if (System.currentTimeMillis() > lastClearTime + clearInterval) {
                this.clearData();
                this.lastClearTime = System.currentTimeMillis();
            }

            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public synchronized void start() {
        super.start();
        this.doRun = true;
    }

    public void interrupt() {
        this.doRun = false;
    }

    private synchronized void clearData() {
        for (K key : this.data.keySet()) {
            if (this.data.get(key).isOutdated()) {
                this.data.remove(key);
            }
        }
    }


    // -----  Map methods  -----

    public synchronized void put(K key, V value, Timestamp endTime) {
        this.data.put(key, new TMValue<>(value, endTime));
    }

    public synchronized void put(K key, V value, long duration) {
        this.put(key, value, new Timestamp(System.currentTimeMillis() + duration));
    }

    public synchronized V get(K key) {
        TMValue<V> value = this.data.get(key);
        if (value != null)
            return value.getValue();

        return null;
    }


    private class TMValue<V> {

        @Getter
        private V value;
        private Timestamp endTime;

        public TMValue(V value, Timestamp endTime) {
            this.value = value;
            this.endTime = endTime;
        }

        /**
         * Returns if the entry is outdated
         *
         * @return
         */
        public boolean isOutdated() {
            return new Timestamp(System.currentTimeMillis()).after(this.endTime);
        }

    }

}
