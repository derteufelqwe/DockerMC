package de.derteufelqwe.commons.misc;

import lombok.Getter;

import javax.annotation.CheckForNull;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TimeoutPermissionStore<K> extends Thread {

    private final Map<K, Set<TOPerm>> data = new ConcurrentHashMap<>();

    /**
     * In ms
     */
    private final int clearInterval;

    private long lastClearTime = System.currentTimeMillis();

    private boolean doRun = true;


    public TimeoutPermissionStore(int clearInterval) {
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
        synchronized (this.data) {
            for (K key : this.data.keySet()) {
                Set<TOPerm> perms = this.data.get(key);
                perms.removeIf(TOPerm::isOutdated);
            }
        }
    }


    // -----  Map methods  -----

    public synchronized void add(K key, String value, Timestamp endTime) {
        if (!this.data.containsKey(key)) {
            this.data.put(key, Collections.synchronizedSet(new HashSet<>()));
        }

        this.data.get(key).add(new TOPerm(value, endTime));
    }

    public synchronized void add(K key, String value, long duration) {
        this.add(key, value, new Timestamp(System.currentTimeMillis() + duration));
    }

    @CheckForNull
    public synchronized Set<String> getPerms(K key) {
        Set<TOPerm> perms = this.data.get(key);
        if (perms == null)
            return null;

        return perms.stream()
                .map(p -> p.getValue())
                .collect(Collectors.toSet());
    }

    public synchronized boolean contains(K key, String value) {
        Set<String> perms = this.getPerms(key);
        if (perms == null)
            return false;

        return perms.contains(value);
    }

    public synchronized void remove(K key) {
        this.data.remove(key);
    }


    private class TOPerm {

        @Getter
        private String value;
        private Timestamp endTime;

        public TOPerm(String value, Timestamp endTime) {
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
