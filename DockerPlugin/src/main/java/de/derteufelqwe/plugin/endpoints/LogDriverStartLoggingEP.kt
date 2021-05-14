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
    /**
     * Time in ms to wait for the container to appear in the DB
     */
    private val CONTAINER_DB_AWAIT_TIMEOUT = 20000

    private val sessionBuilder = DMCLogDriver.getSessionBuilder()
    private val log = LogManager.getLogger(javaClass)
    private val threadPool = DMCLogDriver.getThreadPool()
    private val logfileConsumers = DMCLogDriver.getLogfileConsumers()


    override fun process(request: RStartLogging): StartLogging {
        val file = request.file
        val containerID = request.info.containerID
        val containerType = extractContainerType(request?.info)

        if (containerType == LogConsumer.Type.NODE_WATCHER) {
            this.addNodeWatcherContainerToDB(request.info)

        } else {
//            injectContainerToDB(request.info)
            if (!awaitContainerInDB(containerID)) {
                return StartLogging("Failed to find container $containerID in the DB after $CONTAINER_DB_AWAIT_TIMEOUT ms.")
            }
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

    /**
     * Polls the DB for the container to have an entry in it
     *
     * @param containerID
     */
    private fun awaitContainerInDB(containerID: String): Boolean {
        val tStart = System.currentTimeMillis()

        return sessionBuilder.execute<Boolean> { session: Session ->
            while (System.currentTimeMillis() - tStart < CONTAINER_DB_AWAIT_TIMEOUT) {
                if (checkIfContainerExists(session, containerID)) {
                    return@execute true
                }
//                if (session.get(DBContainer::class.java, containerID) != null) {
//                    return@execute true;
//                }

                try {
                    TimeUnit.MILLISECONDS.sleep(250)
                } catch (e: InterruptedException) {
                    return@execute false
                }
            }
            false
        }
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


    /**
     * DEBUG ONLY!
     * Adds the container to the DB. This is useful when the container doesn't get added by the NodeWatcher because it's
     * a non dockermc test container.
     *
     * @param info
     */
    private fun injectContainerToDB(info: RStartLogging.Info) {
        sessionBuilder.execute { session ->
            val node = session.get(Node::class.java, "asdfasdf")
            val dbService = session.get(DBService::class.java, "debugserviceid")
            val dbContainer = DBContainer(info.containerID)

            dbContainer.name = info.containerName
            dbContainer.node = node
            dbContainer.service = dbService
            session.persist(dbContainer)

            log.warn("DEBUG FEATURE: Injected container to DB.")
        }
    }

}