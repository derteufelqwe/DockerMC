package de.derteufelqwe.plugin.endpoints

import de.derteufelqwe.commons.Constants
import de.derteufelqwe.commons.hibernate.objects.DBContainer
import de.derteufelqwe.commons.hibernate.objects.DBService
import de.derteufelqwe.commons.hibernate.objects.NWContainer
import de.derteufelqwe.commons.hibernate.objects.Node
import de.derteufelqwe.plugin.DMCLogDriver
import de.derteufelqwe.plugin.log.LogConsumer
import de.derteufelqwe.plugin.log.LogDownloadEntry
import de.derteufelqwe.plugin.messages.LogDriver.RStartLogging
import de.derteufelqwe.plugin.messages.LogDriver.StartLogging
import de.derteufelqwe.plugin.misc.DBQueries.checkIfContainerExists
import org.apache.logging.log4j.LogManager
import org.hibernate.Session
import java.io.Serializable
import java.util.concurrent.TimeUnit


class LogDriverStartLoggingEP(data: String?) : Endpoint<RStartLogging, StartLogging>(data) {

    private val sessionBuilder = DMCLogDriver.getSessionBuilder()
    private val log = LogManager.getLogger(javaClass)
    private val threadPool = DMCLogDriver.getThreadPool()
    private val logfileConsumers = DMCLogDriver.getLogfileConsumers()


    override fun process(request: RStartLogging): StartLogging {
        val file = request.file
        val containerID = request.info.containerID
        val containerType = extractContainerType(request.info)

        if (containerType == LogConsumer.Type.NODE_WATCHER) {
            this.addNodeWatcherContainerToDB(request.info)

        } else {
            this.addContainerToDB(request.info)
        }

        val logConsumer = LogConsumer(file, containerID, containerType)
        val future = threadPool.submit(logConsumer)
        logfileConsumers[file] = LogDownloadEntry(logConsumer, future)

        try {
            TimeUnit.MILLISECONDS.sleep(250)

        } catch (e: InterruptedException) {
            log.error("StartLogging sleep interrupted.")
        }

        return StartLogging()
    }

    override fun getRequestType(): Class<out Serializable?> {
        return RStartLogging::class.java
    }

    override fun getResponseType(): Class<out Serializable?> {
        return StartLogging::class.java
    }

    private fun addContainerToDB(infos: RStartLogging.Info) {
        sessionBuilder.execute { session ->
            val dbContainer = DBContainer(
                infos.containerID,
                infos.containerName.substring(1),
                infos.containerImageName
            )

            session.saveOrUpdate(dbContainer);
        }

        log.debug("Added Container ${infos.containerID} to DB")
    }

    private fun addNodeWatcherContainerToDB(infos: RStartLogging.Info) {
        val added = sessionBuilder.execute<Boolean> { session ->
            if (session.get(NWContainer::class.java, infos.containerID) != null) {
                return@execute false
            }

            val nwContainer = NWContainer(
                id = infos.containerID,
                name = infos.containerName.substring(1),
                nodeID = getNodeIDFromEnvs(infos.containerEnv),
                startTime = infos.containerCreated,
            )

            session.persist(nwContainer)
            return@execute true
        }

        if (added) {
            log.info("Added NWContainer ${infos.containerID} to DB")
        }
    }

    private fun extractContainerType(infos: RStartLogging.Info?): LogConsumer.Type {
        val containerType = infos?.containerLabels?.get(Constants.CONTAINER_IDENTIFIER_KEY)

        if (containerType != null && containerType == Constants.ContainerType.NODE_WATCHER.name) {
            return LogConsumer.Type.NODE_WATCHER
        }

        return LogConsumer.Type.NORMAL
    }

    private fun getNodeIDFromEnvs(envs: List<String>): String? {
        for (env in envs) {
            if (env.startsWith("NODE_ID=")) {
                return env.substring(8)
            }
        }

        return null;
    }

}