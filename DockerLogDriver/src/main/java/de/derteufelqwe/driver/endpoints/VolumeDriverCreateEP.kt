package de.derteufelqwe.driver.endpoints

import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.driver.DMCLogDriver
import de.derteufelqwe.driver.messages.VolumeDriver
import java.io.File
import java.io.Serializable
import java.sql.Timestamp

class VolumeDriverCreateEP(data: String?) : Endpoint<VolumeDriver.RCreate, VolumeDriver.Create>(data) {

    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();


    override fun process(request: VolumeDriver.RCreate): VolumeDriver.Create {
        val file = File(DMCLogDriver.VOLUME_PATH + request.name)
        file.mkdir()

        sessionBuilder.execute() { session ->
            val dbVolume = Volume(request.name, Timestamp(System.currentTimeMillis()))
            session.persist(dbVolume)
        };

        println("Created volume ${request.name}")
        return VolumeDriver.Create()
    }

    override fun getRequestType(): Class<out Serializable> {
        return VolumeDriver.RCreate::class.java
    }

    override fun getResponseType(): Class<out Serializable> {
        return VolumeDriver.Create::class.java
    }

}