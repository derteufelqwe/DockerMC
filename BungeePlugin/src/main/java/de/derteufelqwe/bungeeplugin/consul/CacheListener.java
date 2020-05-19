package de.derteufelqwe.bungeeplugin.consul;

import com.orbitz.consul.cache.ConsulCache;

import java.util.*;

public class CacheListener<A, B> implements ConsulCache.Listener<A, B> {

    private Map<A, B> storage = new HashMap<>();
    private List<ICacheChangeListener<A, B>> listeners = new ArrayList<>();

    public CacheListener() {

    }

    @Override
    public void notify(Map<A, B> map) {
        // Add / modify Entries
        for (A key : map.keySet()) {
            if (!this.storage.containsKey(key)) {
                this.storage.put(key, map.get(key));
                this.addEntry(key, map.get(key));

            } else {
                if (!this.storage.get(key).equals(map.get(key))) {
                    this.storage.put(key, map.get(key));
                    this.modifyEntry(key, map.get(key));
                }
            }
        }

        // Remove Entries
        Set<A> keys = new HashSet<>(this.storage.keySet());
        keys.removeAll(map.keySet());
        for (A key : keys) {
            this.removeEntry(key, this.storage.get(key));
            this.storage.remove(key);
        }

    }


    private void addEntry(A key, B value) {
        for (ICacheChangeListener<A, B> listener : this.listeners) {
            listener.onAddEntry(key, value);
        }
    }

    private void removeEntry(A key, B value) {
        for (ICacheChangeListener<A, B> listener : this.listeners) {
            listener.onRemoveEntry(key, value);
        }
    }

    private void modifyEntry(A key, B value) {
        for (ICacheChangeListener<A, B> listener : this.listeners) {
            listener.onModifyEntry(key, value);
        }
    }


    /**
     * Returns a copy of the internal storage
     * @return
     */
    public Map<A, B> getStorage() {
        return new HashMap<>(this.storage);
    }


    public void addListener(ICacheChangeListener<A, B> listener) {
        this.listeners.add(listener);
    }

    public void removeListener(ICacheChangeListener<A, B> listener) {
        if (this.listeners.contains(listener)) {
            this.listeners.remove(listener);
        }
    }

}

