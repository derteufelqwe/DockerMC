package de.derteufelqwe.ServerManager.config.configs.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerObjBase {

    // Name prefix
    private String name;
    // Image
    private String image;
    // Amount of replicas
    private int replications;
    // RAM limit per task, like 2G or 512M
    private String ramLimit;
    // CPU limit per task like 1
    private String cpuLimit;

}
