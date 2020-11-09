package de.derteufelqwe.commons.consul;

import com.orbitz.consul.cache.ConsulCache;

import java.util.*;

/**
 * Handles the creation and data management of consul listeners.
 * To set this up you need to create a consul cache listener like ServiceCatalogCache.newCache(catalogClient, "name");
 * Create a CacheListener and add it as a listener to the consul cache.
 * To respond to changes from CacheListener you need to add a ICacheChangeListeners to it.
 * The data flow it like this:
 *      Consul cache listener
 *              V
 *      CacheListener
 *              V
 *      ICacheChangeListener
 *
 * @param <A> Type A of the cache listener
 * @param <B> Type B of the cache listener
 */
public abstract class ConsulCacheDistributor<A, B> implements ConsulCache.Listener<A, B> {

    private Map<A, B> storage = new HashMap<>();
    private List<ICacheChangeListener<A, B>> listeners = new ArrayList<>();

    public ConsulCacheDistributor() {

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

