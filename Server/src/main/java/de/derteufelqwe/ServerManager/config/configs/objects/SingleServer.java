package de.derteufelqwe.ServerManager.config.configs.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SingleServer {

    // Name prefix
    private String name;
    // Image
    private String image;
    // Soft playerlimit
    private int softPlayerLimit;
    // RAM limit per task, like 2G or 512M
    private String ramLimit;
    // CPU limit per task like 1
    private String cpuLimit;

}
