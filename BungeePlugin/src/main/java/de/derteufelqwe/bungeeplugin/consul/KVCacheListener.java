package de.derteufelqwe.bungeeplugin.consul;

import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.Value;
import de.derteufelqwe.commons.consul.ConsulCacheDistributor;

/**
 * This class can be treated like the consul KVCache with the exception, that only value changes get notified.
 * Programs probably only need one instance of this class and add as many listeners as they want.
 * Remember to call the init method.
 */
public class KVCacheListener extends ConsulCacheDistributor<String, Value> {

    private KeyValueClient kvClient;
    private String rootPath;
    private KVCache kvCache;


    public KVCacheListener(KeyValueClient kvClient, String rootPath) {
        super();
        this.kvClient = kvClient;
        this.rootPath = rootPath;
    }


    public void init() {
        this.kvCache = KVCache.newCache(kvClient, rootPath);
        this.kvCache.addListener(this);
    }

    public void start() {
        this.kvCache.start();
    }

    public void stop() {
        this.kvCache.stop();
    }

}
