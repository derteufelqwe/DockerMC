package de.derteufelqwe.plugin.endpoints

import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.Volume
import de.derteufelqwe.plugin.misc.DBQueries
import de.derteufelqwe.plugin.DMCLogDriver
import de.derteufelqwe.plugin.messages.VolumeDriver
import org.apache.logging.log4j.LogManager
import java.io.Serializable

class VolumeDriverListEP(data: String?) : Endpoint<VolumeDriver.RList, VolumeDriver.List>(data) {

    private val log = LogManager.getLogger(javaClass)
    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();


    override fun process(request: VolumeDriver.RList): VolumeDriver.List {
        // This call somehow can't be a method reference as this will cause weired kotlin NoSuchMethodExceptions
        val volumes = sessionBuilder.execute<List<Volume>> { session ->
            return@execute DBQueries.getAllVolumes(session)
        }

        val resultList = volumes
            .map { VolumeDriver.Volume(it.id, it.rootFolder?.name) }

        return VolumeDriver.List(resultList)
    }

    override fun getRequestType(): Class<out Serializable?> {
        return VolumeDriver.RList::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return VolumeDriver.List::class.java
    }
}