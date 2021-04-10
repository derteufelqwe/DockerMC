package de.derteufelqwe.ServerManager.cli.system;

import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.utils.Commons;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@CommandLine.Command(name = "reloadConfig", description = "Reloads all config files")
@Log4j2
public class ReloadConfigCmd implements Runnable {

    private final Commons commons = ServerManager.getCommons();

    @Override
    public void run() {
        ServerManager.mainConfig.load();
        commons.reloadServerConfig();
        log.info("Reloaded config files.");
    }
}
