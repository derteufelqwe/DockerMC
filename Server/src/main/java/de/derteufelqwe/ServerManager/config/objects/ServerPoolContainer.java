package de.derteufelqwe.ServerManager.config.objects;


import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * This class is a wrapper for a HashMap storing the pool servers
 */
@Data
@NoArgsConstructor
public class ServerPoolContainer implements Iterable<ServerPool> {

    private Map<String, ServerPool> data = new HashMap<>();


    public void addServer(ServerPool pool) {
        this.data.put(pool.getName(), pool);
    }

    public void removeServer(String name) {
        this.data.remove(name);
    }

    public ServerPool getServer(String name) {
        return this.data.get(name);
    }

    public void clear() {
        this.data.clear();
    }

    @Override
    public Iterator<ServerPool> iterator() {
        return this.data.values().iterator();
    }

    @Override
    public void forEach(Consumer<? super ServerPool> action) {
        this.data.values().forEach(action);
    }

    @Override
    public Spliterator<ServerPool> spliterator() {
        return this.data.values().spliterator();
    }

}
