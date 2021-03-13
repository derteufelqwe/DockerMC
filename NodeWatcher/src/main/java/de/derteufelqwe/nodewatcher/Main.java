package de.derteufelqwe.nodewatcher;

import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    /*
     * Commands:
     *  cat <(grep 'cpu ' /proc/stat) <(sleep 1 && grep 'cpu ' /proc/stat) | awk -v RS="" '{print ($13-$2+$15-$4)*100/($13-$2+$15-$4+$16-$5)}'
     *  cat /proc/meminfo | grep -A1 "MemTotal"
     */


    @SneakyThrows
    public static void main(String[] args) {
        Logger.getLogger("org.hibernate").setLevel(Level.WARNING);

//        NodeWatcher nodeWatcher = new NodeWatcher("tcp://ubuntu1:2375", new SessionBuilder("admin", "password", "ubuntu1", 5432));
        NodeWatcher nodeWatcher = new NodeWatcher("unix:///var/run/docker.sock", new SessionBuilder("admin", "password", Constants.POSTGRESDB_CONTAINER_NAME, Constants.POSTGRESDB_PORT));


        nodeWatcher.start();

        while (true) {
            TimeUnit.SECONDS.sleep(10);
        }
    }

}
