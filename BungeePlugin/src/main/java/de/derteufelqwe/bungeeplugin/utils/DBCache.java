package de.derteufelqwe.bungeeplugin.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import org.hibernate.Session;

import java.util.concurrent.ExecutionException;

/**
 * Cache for database objects
 */
public class DBCache {

    private final SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();

    private LoadingCache<Long, String> serviceCache;

    public DBCache() {
        this.serviceCache = CacheBuilder.newBuilder().build(new CacheLoader<Long, String>() {
            @Override
            public String load(Long key) throws Exception {
                try (Session session = sessionBuilder.openSession()) {
                    DBService service = session.get(DBService.class, key);

                    return service.getName();
                }
            }
        });
    }


    public String getServiceName(Long id) {
        try {
            return this.serviceCache.get(id);

        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to load service " + id + ".");
        }
    }


}
