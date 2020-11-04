package de.derteufelqwe.ServerManager.config.objects;


import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.config.annotations.Exclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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


    /**
     * Removes all obsolete entries based on available entries in the {@link InfrastructureConfig}
     */
    public void cleanup() {
        List<String> existingNames = ServerManager.CONFIG.get(InfrastructureConfig.class).getPoolServers().stream()
                .map(s -> s.getName())
                .collect(Collectors.toList());

        for (String name : new HashSet<>(this.data.keySet())) {
            if (!existingNames.contains(name)) {
                this.data.remove(name);
            }
        }

    }


}
