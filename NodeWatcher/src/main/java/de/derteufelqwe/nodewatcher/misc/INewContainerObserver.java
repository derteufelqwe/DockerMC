package de.derteufelqwe.nodewatcher.misc;

/**
 * Classes implementing this, can be notified, when a new container starts
 */
public interface INewContainerObserver {

    /**
     * Called when the container is already present in the database
     * @param containerId
     */
    void onNewContainer(String containerId);

}
