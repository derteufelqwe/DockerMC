package de.derteufelqwe.nodewatcher.misc;

/**
 * Classes implementing this can be notified when services get started or stopped
 */
public interface IServiceObserver {

    void onServiceStart(String id);

    void onServiceStop(String id);

}
