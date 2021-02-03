package de.derteufelqwe.bungeeplugin.runnables;

import net.md_5.bungee.api.Callback;

/**
 * Default callback that does nothing
 * @param <V>
 */
public class DefaultCallback<V> implements Callback<V> {

    @Override
    public void done(V v, Throwable throwable) {

    }
}
