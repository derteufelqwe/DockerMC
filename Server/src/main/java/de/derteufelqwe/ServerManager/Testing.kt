package de.derteufelqwe.ServerManager

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jcraft.jsch.*
import de.derteufelqwe.ServerManager.config.MainConfig
import de.derteufelqwe.commons.config.Config
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter
import de.derteufelqwe.commons.misc.RemoteFile
import de.derteufelqwe.commons.misc.VFile
import java.io.*
import java.nio.CharBuffer
import java.nio.charset.Charset


fun test() {
    val jsch = JSch()
    jsch.setKnownHosts(System.getProperty("user.home") + "/.ssh/known_hosts")
    val session = jsch.getSession("arne", "ubuntu1")

    session.userInfo = object : UserInfo {
        override fun getPassphrase() = ""

        override fun getPassword() = "admin"

        override fun promptPassword(message: String?): Boolean {
            return true
        }

        override fun promptPassphrase(message: String?): Boolean {
            return false
        }

        override fun promptYesNo(message: String?): Boolean {
            return true
        }

        override fun showMessage(message: String?) {
            println("SSH message: $message")
        }
    }

    session.connect()
    try {
        println("Connected")
        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect()
        val input = channel.get("/home/arne/info.txt")
        val text = input.bufferedReader(Charset.defaultCharset()).readText()
        val a = channel.ls("/home/arne/info.txt")
        val b = channel.lstat("/home/arne/info.txt")

        channel.exit()

    } finally {
        session.disconnect()
    }
}


fun main() {

    val file = RemoteFile("ubuntu1", "arne", "admin", "/home/arne/config.yml")
    val cfg = Config(file, DefaultYamlConverter(), DefaultGsonProvider(), MainConfig())

    cfg.save()

    file.cleanup()
    println("Done")
}


