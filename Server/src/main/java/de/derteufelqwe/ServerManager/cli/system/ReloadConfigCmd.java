package de.derteufelqwe.ServerManager.cli.system;

import com.google.inject.Inject;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.utils.Commons;
import de.derteufelqwe.commons.config.Config;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@CommandLine.Command(name = "reloadConfig", description = "Reloads all config files")
@Log4j2
public class ReloadConfigCmd implements Runnable {

    @Inject private Config<MainConfig> mainConfig;
    @Inject private Commons commons;

    @Override
    public void run() {
        mainConfig.load();
        commons.reloadServerConfig();
        log.info("Reloaded config files.");
    }
}
