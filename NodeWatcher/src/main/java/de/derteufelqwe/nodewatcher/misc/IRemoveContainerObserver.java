package de.derteufelqwe.nodewatcher.misc;

/**
 * Classes implementing this, can be notified, when a new container stopped
 */
public interface IRemoveContainerObserver {

    void onRemoveContainer(String containerId);

}
