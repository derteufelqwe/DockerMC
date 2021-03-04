package de.derteufelqwe.ServerManager;

import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import lombok.SneakyThrows;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

public class Testing {

    @SneakyThrows
    public static void main(String[] args) {
        DockerRegistryAPI api = new DockerRegistryAPI("https://registry.swarm", "admin", "root");
        for (int i = 0; i < 1000; i++) {
            api.getManifest("testmc", "latest");
            System.out.print("\rIteration " + i);
        }
        System.out.println("Done");
    }

}
