package minecraftplugin.minecraftplugin.teleportsigns;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.cache.ServiceCatalogCache;
import com.orbitz.consul.model.catalog.CatalogService;
import com.orbitz.consul.model.kv.Value;
import de.derteufelqwe.commons.consul.CacheListener;
import de.derteufelqwe.commons.consul.ICacheChangeListener;
import org.bukkit.material.Sign;

import java.util.Collections;
import java.util.List;

/**
 * Watches if a server get restarted or if the player count changes and updates the teleport signs accordingly.
 */
public class TeleportSignWatcher implements ICacheChangeListener<String, CatalogService> {

    private CatalogClient catalogClient;
    private ServiceCatalogCache serviceCatalogCache;
    private CacheListener<String, CatalogService> cacheListener = new CacheListener<>();

    public TeleportSignWatcher(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;

        this.cacheListener.addListener(this);
        this.serviceCatalogCache = ServiceCatalogCache.newCache(catalogClient, "minecraft");
        this.serviceCatalogCache.addListener(this.cacheListener);
    }

    public void start() {
        this.serviceCatalogCache.start();
    }

    public void stop() {
        this.serviceCatalogCache.stop();
    }

    @Override
    public void onAddEntry(String key, CatalogService value) {
        System.out.println("Add " + key);
    }

    @Override
    public void onModifyEntry(String key, CatalogService value) {

    }

    @Override
    public void onRemoveEntry(String key, CatalogService value) {
        System.out.println("Remove " + key);
    }


    private void setServerWaiting(Sign sign) {
        List<String> list;
    }

    private void setServerRunning(Sign sign) {

    }

}
