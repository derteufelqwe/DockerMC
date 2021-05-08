package de.derteufelqwe.plugin.endpoints

import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.plugin.DMCLogDriver
import de.derteufelqwe.plugin.messages.VolumeDriver
import de.derteufelqwe.plugin.misc.DBQueries
import org.apache.logging.log4j.LogManager
import java.io.Serializable

class VolumeDriverRemoveEP(data: String?) : Endpoint<VolumeDriver.RRemove, VolumeDriver.Remove>(data) {

    private val log = LogManager.getLogger(javaClass)
    private val sessionBuilder = DMCLogDriver.getSessionBuilder()


    override fun process(request: VolumeDriver.RRemove): VolumeDriver.Remove {
        var error = ""
        try {
            error = sessionBuilder.execute<String> { session ->
                val volume = session.get(Volume::class.java, request.volumeName)
                    ?: return@execute "Volume ${request.volumeName} not found."

                DBQueries.deleteVolume(session, volume)
                log.info("Deleted volume ${request.volumeName}")

                return@execute ""
            }

        } catch (e: Exception) {
            error = e.message ?: "Deletion failed with no error."
        }

        return VolumeDriver.Remove(error)
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RRemove::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Remove::class.java
    }
}