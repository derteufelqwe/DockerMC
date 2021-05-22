package de.derteufelqwe.plugin.endpoints

import de.derteufelqwe.commons.Constants
import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFolder
import de.derteufelqwe.plugin.DMCLogDriver
import de.derteufelqwe.plugin.messages.VolumeDriver
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.Serializable
import java.sql.Timestamp

class VolumeDriverCreateEP(data: String?) : Endpoint<VolumeDriver.RCreate, VolumeDriver.Create>(data) {

    private val log = LogManager.getLogger(javaClass)
    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();


    override fun process(request: VolumeDriver.RCreate): VolumeDriver.Create {
        val file = File(DMCLogDriver.VOLUME_PATH, request.name)
        file.mkdir()

        sessionBuilder.execute { session ->
            val dbVolume = Volume(request.name, Timestamp(System.currentTimeMillis()))
            dbVolume.groupName = request.opts.getOrDefault(Constants.VOLUME_GROUPNAME_KEY, "NO_GROUP_NAME_SUPPLIED")
            val rootFolder = VolumeFolder("/")
            rootFolder.volume = dbVolume

            session.persist(dbVolume)
            session.persist(rootFolder)
        };

        log.info("Created volume ${request.name}")
        return VolumeDriver.Create()
    }

    override fun getRequestType(): Class<out Serializable> {
        return VolumeDriver.RCreate::class.java
    }

    override fun getResponseType(): Class<out Serializable> {
        return VolumeDriver.Create::class.java
    }

}