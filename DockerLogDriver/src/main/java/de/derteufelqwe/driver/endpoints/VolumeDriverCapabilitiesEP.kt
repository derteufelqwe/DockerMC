package de.derteufelqwe.driver.endpoints

import de.derteufelqwe.driver.messages.VolumeDriver
import java.io.Serializable

class VolumeDriverCapabilitiesEP(data: String?) : Endpoint<VolumeDriver.RCapabilities, VolumeDriver.Capabilities>(data) {

    override fun process(request: VolumeDriver.RCapabilities): VolumeDriver.Capabilities {
        return VolumeDriver.Capabilities()
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RCapabilities::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Capabilities::class.java
    }
}