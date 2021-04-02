package de.derteufelqwe.ServerManager.cli.system;

import com.github.dockerjava.api.model.Swarm;
import com.github.dockerjava.api.model.SwarmInfo;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.spring.Commons;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@CommandLine.Command(name = "joinNode", description = "Information on how to join a node to the swarm")
@Log4j2
public class JoinNodeCmd implements Runnable {

    private final Docker docker = ServerManager.getDocker();

    @CommandLine.Option(names = {"-q", "--quiet"}, description = "Only displays the required command")
    private boolean quiet = false;

    @Override
    public void run() {
        Swarm swarm = docker.getDocker().inspectSwarmCmd().exec();
        SwarmInfo swarmInfo = docker.getDocker().infoCmd().exec().getSwarm();

        if (!quiet) {
            log.info("To join a new node to the swarm enter the following command on the host you want to join.");
        }

        log.info("docker swarm join --token {} {}:2377", swarm.getJoinTokens().getWorker(), swarmInfo.getNodeAddr());
    }
}
