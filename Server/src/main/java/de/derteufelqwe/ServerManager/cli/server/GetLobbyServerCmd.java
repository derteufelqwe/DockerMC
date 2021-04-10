package de.derteufelqwe.ServerManager.cli.server;

import de.derteufelqwe.ServerManager.DBQueries;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "getLobby", description = "Shows the name of the lobby server as stored in redis")
@Log4j2
public class GetLobbyServerCmd implements Runnable {


    @Override
    public void run() {
        String lobbyServer = "";

        if (lobbyServer == null || lobbyServer.equals("")) {
            log.warn("LobbyServer name not configured.");

        } else {
            log.info("LobbyServer name: '{}'.", lobbyServer);
        }
    }
}
