package de.derteufelqwe.plugin.endpoints

import de.derteufelqwe.commons.Utils
import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.plugin.DMCLogDriver
import de.derteufelqwe.plugin.messages.VolumeDriver
import java.io.File
import java.io.Serializable
import javax.persistence.NoResultException

class VolumeDriverGetEP(data: String?) : Endpoint<VolumeDriver.RGet, VolumeDriver.Get>(data) {

    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();


    override fun process(request: VolumeDriver.RGet): VolumeDriver.Get {
        val file = File(DMCLogDriver.VOLUME_PATH, request.volumeName)
        var result = VolumeDriver.Get(null, "Volume ${request.volumeName} not found")

        sessionBuilder.execute() { session ->
            try {
                val vol = session.get(Volume::class.java, request.volumeName)
                if (vol != null) {
                    result = VolumeDriver.Get(
                        VolumeDriver.Volume(request.volumeName, file.absolutePath, created = Utils.toISO8601(vol.created))
                    )
                }

            } catch (_: NoResultException) {

            }
        }

        return result
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RGet::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Get::class.java
    }
}