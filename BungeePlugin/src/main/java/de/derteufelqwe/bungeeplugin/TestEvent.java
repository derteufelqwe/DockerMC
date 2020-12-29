package de.derteufelqwe.bungeeplugin;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.AsyncEvent;

public class TestEvent extends AsyncEvent<TestEvent> {

    public ProxiedPlayer proxiedPlayer;

    public TestEvent(ProxiedPlayer player, Callback<TestEvent> done) {
        super(done);
        this.proxiedPlayer = player;
    }
}
