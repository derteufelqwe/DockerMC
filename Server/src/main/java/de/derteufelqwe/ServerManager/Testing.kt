package de.derteufelqwe.ServerManager

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.UserInfo
import de.derteufelqwe.junitDocker.JUnitService
import sun.rmi.server.UnicastRef
import sun.rmi.transport.LiveRef
import sun.rmi.transport.tcp.TCPEndpoint
import java.lang.reflect.Proxy
import java.nio.charset.Charset
import java.rmi.registry.LocateRegistry
import java.rmi.server.RemoteObject
import kotlin.system.measureTimeMillis


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
    val port = 49183
    val registry = LocateRegistry.getRegistry("192.168.137.101", port)

    val t1 = measureTimeMillis {
        val service = registry.lookup("JUnitTestService") as JUnitService
    }

    val t2 = measureTimeMillis {
        val service = registry.lookup("JUnitTestService") as JUnitService
    }

    println("Took $t1 ms and $t2 ms.")

    // This massacre reflects into the proxy object and changes the return port for the RMI calls.
//    val refF = RemoteObject::class.java.getDeclaredField("ref")
//    refF.isAccessible = true
//    val ref = refF.get(Proxy.getInvocationHandler(service)) as UnicastRef
//    val liveRef = ref.liveRef
//
//    val epF = LiveRef::class.java.getDeclaredField("ep")
//    epF.isAccessible = true
//
//    val endpoint = epF.get(liveRef) as TCPEndpoint
//    val portF = TCPEndpoint::class.java.getDeclaredField("port")
//    portF.isAccessible = true
//    portF.setInt(endpoint, port)
//
//    val hostF = TCPEndpoint::class.java.getDeclaredField("host")
//    hostF.isAccessible = true
//    hostF.set(endpoint, "ubuntu1")

}


