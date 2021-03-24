package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.messages.Plugin;

import java.io.Serializable;

public class PluginActivateEP extends Endpoint<Plugin.RActivate, Plugin.Activate> {

    public PluginActivateEP(String data) {
        super(data);
    }


    @Override
    protected Class<? extends Serializable> getRequestType() {
        return Plugin.RActivate.class;
    }

    @Override
    protected Class<? extends Serializable> getResponseType() {
        return Plugin.Activate.class;
    }

    @Override
    protected Plugin.Activate process(Plugin.RActivate request) {
        return new Plugin.Activate();
    }
}
