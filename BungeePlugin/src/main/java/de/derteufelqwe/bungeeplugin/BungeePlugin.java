package de.derteufelqwe.bungeeplugin;

import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public final class BungeePlugin extends Plugin {

    @Getter
    private static Docker docker;
    private ScheduledTask dockerHandler;

    @Override
    public void onEnable() {
        DockerSignalHandler.listenTo("TERM");
        docker = new Docker();

        getProxy().getPluginManager().registerListener(this, new Events());

        System.out.println("Starting eventhandler");
        this.dockerHandler = getProxy().getScheduler().runAsync(this, new DockerEventHandler());
    }

    @Override
    public void onDisable() {
        this.dockerHandler.cancel();
    }
}
