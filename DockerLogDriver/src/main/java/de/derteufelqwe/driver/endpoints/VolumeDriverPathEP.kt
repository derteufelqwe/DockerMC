package de.derteufelqwe.driver.endpoints

import de.derteufelqwe.driver.DMCLogDriver
import de.derteufelqwe.driver.messages.VolumeDriver
import java.io.Serializable

class VolumeDriverPathEP(data: String?) : Endpoint<VolumeDriver.RPath, VolumeDriver.Path>(data) {

    override fun process(request: VolumeDriver.RPath): VolumeDriver.Path {
        return VolumeDriver.Path(DMCLogDriver.VOLUME_PATH + request.volumeName, "")
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RPath::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Path::class.java
    }
}