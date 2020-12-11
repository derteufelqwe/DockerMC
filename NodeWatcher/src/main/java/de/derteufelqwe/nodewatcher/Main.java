package de.derteufelqwe.nodewatcher;

import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Container;
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


    public static void test() {
        SessionBuilder sessionBuilder = new SessionBuilder("admin", "password", "ubuntu1:5432", false);
        Session session = sessionBuilder.openSession();
        Transaction tx = session.beginTransaction();

//        Node n = session.get(Node.class, "testId");
        Container c = session.get(Container.class, "testId");
        System.out.println(c);
        System.out.println(c.getNode());

        tx.commit();
        session.close();

    }

    @SneakyThrows
    public static void main(String[] args) {
        Logger.getLogger("org.hibernate").setLevel(Level.WARNING);

        NodeWatcher nodeWatcher = new NodeWatcher("tcp://ubuntu1:2375", "ubuntu1:5432");
//        NodeWatcher nodeWatcher = new NodeWatcher("unix:///var/run/docker.sock", String.format("%s:%s", Constants.POSTGRESDB_CONTAINER_NAME, Constants.POSTGRESDB_PORT));

        nodeWatcher.start();

        System.out.println("Starting to listen for container deaths...");

        while (true) {
            TimeUnit.SECONDS.sleep(10);
        }
    }

}
