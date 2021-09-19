package de.derteufelqwe.ServerManager.utils

import java.io.File
import java.util.*

class DevConfig {

    companion object {

        private val FILE_NAME = "dev.properties"
        // Property keys
        val REGISTRY_CERT_OVERRIDE = "registryCertOverride"

        fun exists(): Boolean {
            val file = File(FILE_NAME)
            return file.exists()
        }

        fun read(): Properties {
            val file = File(FILE_NAME)
            val props = Properties()
            props.load(file.reader())

            return props
        }

    }

}