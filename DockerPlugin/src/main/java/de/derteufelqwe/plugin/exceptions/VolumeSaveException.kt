package de.derteufelqwe.plugin.exceptions

import de.derteufelqwe.commons.exceptions.DockerMCException

/**
 * Thrown when the volume plugin failed to save a volume
 */
class VolumeSaveException : DockerMCException {

    var error: String = ""

    constructor(volumeName: String, msg: String) : super("Saving volume $volumeName failed. Error: $msg") {
        this.error = msg
    }

    constructor(
        volumeName: String,
        exception: Throwable
    ) : super("Exception occured while saving volume $volumeName. Error: ${exception.message}") {
        exception.message?.let {
            this.error = it
        }
    }

}