package de.derteufelqwe.plugin.endpoints

import de.derteufelqwe.plugin.messages.VolumeDriver
import java.io.Serializable

class VolumeDriverRemoveEP(data: String?) : Endpoint<VolumeDriver.RRemove, VolumeDriver.Remove>(data) {

    override fun process(request: VolumeDriver.RRemove): VolumeDriver.Remove {
        return VolumeDriver.Remove()
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RRemove::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.Remove::class.java
    }
}