package de.derteufelqwe.bungeeplugin.docker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DockerConfig {

    private String host;
    private int port;
    private String name;
    private Thread thread;
    private Docker dockerInst;

}
