package de.derteufelqwe.ServerManager.config.configs.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BungeeProxy {

    // Name of the Proxy. Currently irrelevant.
    private String name;
    // Imagename without registry.swarm
    private String image;
    // Port of the proxy
    private int port;

}
