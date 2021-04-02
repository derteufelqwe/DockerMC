package de.derteufelqwe.nodewatcher.misc;

public interface IContainerObserver {

    /**
     * Called when the container is already present in the database
     * @param containerId
     */
    void onNewContainer(String containerId);

    void onRemoveContainer(String containerId);

}
