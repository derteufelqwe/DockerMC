package de.derteufelqwe.bungeeplugin;

import net.md_5.bungee.api.plugin.Plugin;

public final class BungeePlugin extends Plugin {


    @Override
    public void onEnable() {
        DockerSignalHandler.listenTo("TERM");

        getProxy().getPluginManager().registerListener(this, new Events());

        System.out.println("Starting eventhandler");
        DockerPoolHandler dockerPoolHandler = new DockerPoolHandler();
        dockerPoolHandler.start();

        System.out.println("Started eventhandler.");
    }

    @Override
    public void onDisable() {

    }


}
