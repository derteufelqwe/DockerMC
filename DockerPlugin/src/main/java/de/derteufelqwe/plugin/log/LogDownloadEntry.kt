package de.derteufelqwe.plugin.log

import java.util.concurrent.Future

data class LogDownloadEntry(
    val consumer: LogConsumer? = null,
    val future: Future<*>? = null
)