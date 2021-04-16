package de.derteufelqwe.driver.endpoints

import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.driver.DBQueries
import de.derteufelqwe.driver.DMCLogDriver
import de.derteufelqwe.driver.messages.VolumeDriver
import org.apache.logging.log4j.LogManager
import java.io.Serializable

class VolumeDriverListEP(data: String?) : Endpoint<VolumeDriver.RList, VolumeDriver.List>(data) {

    private val log = LogManager.getLogger(javaClass)
    private val sessionBuilder: SessionBuilder = DMCLogDriver.getSessionBuilder();


    override fun process(request: VolumeDriver.RList): VolumeDriver.List {
        val volumes = sessionBuilder.execute(DBQueries::getAllVolumes)

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