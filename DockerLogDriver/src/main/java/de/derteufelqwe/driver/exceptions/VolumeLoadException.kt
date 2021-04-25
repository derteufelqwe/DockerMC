package de.derteufelqwe.driver.exceptions

import de.derteufelqwe.commons.exceptions.DockerMCException

/**
 * Thrown when the volume plugin failed to load a volume
 */
class VolumeLoadException : DockerMCException {

    var error: String = ""

    constructor(volumeName: String, msg: String) : super("Loading volume $volumeName failed. Error: $msg") {
        this.error = msg
    }

    constructor(
        volumeName: String,
        exception: Throwable
    ) : super("Exception occurred while loading volume $volumeName. Error: ${exception.message}") {
        exception.message?.let {
            this.error = it
        }
    }

}