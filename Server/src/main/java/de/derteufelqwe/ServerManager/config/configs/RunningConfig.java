package de.derteufelqwe.ServerManager.config.configs;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RunningConfig {

    private boolean dnsInitialized = false;
    private boolean registryInitialized = false;

}
