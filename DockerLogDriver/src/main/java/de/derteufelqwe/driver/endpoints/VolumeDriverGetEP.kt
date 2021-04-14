package de.derteufelqwe.driver.endpoints

import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.driver.DMCLogDriver
import de.derteufelqwe.driver.messages.VolumeDriver
import java.io.File
import java.io.Serializable
import javax.persistence.NoResultException

class VolumeDriverGetEP(data: String?) : Endpoint<VolumeDriver.RGet, VolumeDriver.Get>(data) {

    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();


    override fun process(request: VolumeDriver.RGet): VolumeDriver.Get {
        val file = File(DMCLogDriver.VOLUME_PATH + request.volumeName)

        var volumeInDB: Boolean = sessionBuilder.execute<Boolean>() { session ->
            try {
                session.get(Volume::class.java, request.volumeName);
                return@execute true

            } catch (e: NoResultException) {
                return@execute false
            }
        }

        return if (!volumeInDB) {
            VolumeDriver.Get(null, "volume not found")

        } else VolumeDriver.Get(
            VolumeDriver.Volume(request.volumeName, file.absolutePath, null),
            "")
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RGet::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Get::class.java
    }
}