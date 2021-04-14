package de.derteufelqwe.driver.endpoints

import de.derteufelqwe.driver.messages.VolumeDriver
import java.io.Serializable

class VolumeDriverListEP(data: String?) : Endpoint<VolumeDriver.RList, VolumeDriver.List>(data) {

    override fun process(request: VolumeDriver.RList): VolumeDriver.List {
        return VolumeDriver.List()
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RList::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.List::class.java
    }
}