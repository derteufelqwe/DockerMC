package de.derteufelqwe.plugin.endpoints

import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.plugin.DMCLogDriver
import de.derteufelqwe.plugin.messages.VolumeDriver
import java.io.Serializable

class VolumeDriverPathEP(data: String?) : Endpoint<VolumeDriver.RPath, VolumeDriver.Path>(data) {

    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();


    override fun process(request: VolumeDriver.RPath): VolumeDriver.Path {
        var result = VolumeDriver.Path("Volume ${request.volumeName} not found.")

        sessionBuilder.execute { session ->
            val vol = session.get(Volume::class.java, request.volumeName);
            if (vol != null) {
                result = VolumeDriver.Path(DMCLogDriver.VOLUME_PATH + request.volumeName, "")
            }
        }

        return result
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RPath::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Path::class.java
    }
}