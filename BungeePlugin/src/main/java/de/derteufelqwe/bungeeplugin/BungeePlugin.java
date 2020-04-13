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

        getProxy().getPluginManager().registerListener(this, new Events());

        System.out.println("Starting eventhandler");
//        this.dockerHandler = getProxy().getScheduler().runAsync(this, new DockerEventHandler());
        DockerPoolHandler dockerPoolHandler = new DockerPoolHandler();
        dockerPoolHandler.start();

        System.out.println("Started eventhandler.");

    }

    @Override
    public void onDisable() {
        this.dockerHandler.cancel();
    }


}
