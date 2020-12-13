package de.derteufelqwe.nodewatcher.misc;

/**
 * Classes implementing this, can be notified, when a new container starts
 */
public interface INewContainerObserver {

    void onNewContainer(String containerId);

}
