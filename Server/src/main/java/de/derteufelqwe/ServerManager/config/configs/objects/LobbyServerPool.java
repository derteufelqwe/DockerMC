package de.derteufelqwe.ServerManager.config.configs.objects;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LobbyServerPool {

    // Name prefix
    private String name;
    // Image
    private String image;
    // Amount of replicas
    private int replications;

}
