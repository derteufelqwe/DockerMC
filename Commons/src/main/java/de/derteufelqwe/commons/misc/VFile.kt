package de.derteufelqwe.commons.misc

import com.jcraft.jsch.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


/**
 * VFile = Variable File. Its source can vary
 */
open class VFile(pathname: String) : File(pathname) {

    open fun getInputStream() : InputStream {
        return this.inputStream()
    }

    open fun getOutputStream() : OutputStream {
        return this.outputStream()
    }

    open fun cleanup() {
        // Nothing to do here
    }

}


class RemoteFile : VFile {

    private var host = ""
    private var user = ""
    private var jsch: JSch = JSch()
    var session: Session? = null

    private val unixPath = path.replace("\\", "/")


    constructor(host: String, user: String, password: String, pathname: String) : super(pathname) {
        this.host = host
        this.user = user

        jsch.setKnownHosts(System.getProperty("user.home") + "/.ssh/known_hosts")
        this.session = jsch.getSession("arne", "ubuntu1")

        session?.userInfo = object : UserInfo {
            override fun getPassphrase() = ""

            override fun getPassword() = password

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

        this.session?.connect()
    }


    override fun cleanup() {
        this.session?.disconnect()
    }

    override fun length(): Long {
        val channel = openSFTPChannel()
        try {
            return channel.lstat(unixPath).size

        } finally {
            channel.exit()
        }
    }

    override fun createNewFile(): Boolean {
        if (this.exists()) {
            return false
        }

        val channel = openSFTPChannel()
        try {
            channel.put(unixPath).close()
            return true

        } finally {
            channel.exit()
        }
    }

    override fun exists(): Boolean {
        val channel = openSFTPChannel()
        try {
            channel.lstat(unixPath)
            return true

        } catch (e: SftpException) {
            if (e.message == "No such file") {
                return false
            }
            throw e

        } finally {
            channel.exit()
        }
    }

    override fun delete(): Boolean {
        val channel = openSFTPChannel()
        try {
            val lstat = channel.lstat(unixPath)
            if (lstat.isDir) {
                channel.rmdir(unixPath)

            } else {
                channel.rm(unixPath)
            }

            return true

        } finally {
            channel.exit()
        }
    }

    override fun isDirectory(): Boolean {
        val channel = openSFTPChannel()
        try {
            return channel.lstat(unixPath).isDir

        } finally {
            channel.exit()
        }
    }

    override fun isFile(): Boolean {
        val channel = openSFTPChannel()
        try {
            val lstat = channel.lstat(unixPath)
            return !lstat.isDir && !lstat.isBlk && !lstat.isLink

        } finally {
            channel.exit()
        }
    }

    override fun canRead(): Boolean {
        TODO("Not easily possible to detect")
    }

    override fun canWrite(): Boolean {
        TODO("Not easily possible to detect")
    }

    override fun mkdir(): Boolean {
        val channel = openSFTPChannel()
        try {
            channel.mkdir(unixPath)
            return true

        } catch (e: SftpException) {
            if (e.message == "No such file") {
                return false
            }
            throw e

        } finally {
            channel.exit()
        }
    }

    override fun mkdirs(): Boolean {
        fun folderExists(channel: ChannelSftp, name: String): Boolean {
            try {
                channel.lstat(name)
                return true

            } catch (e: SftpException) {
                if (e.message == "No such file") {
                    return false
                }
                throw e
            }
        }

        val channel = openSFTPChannel()
        try {
            var workingPath = ""
            for (folder in unixPath.split("/")) {
                workingPath += "/$folder"
                if (folderExists(channel, workingPath)) {
                    continue
                }

                channel.mkdir(workingPath)
            }
            return true

        } finally {
            channel.exit()
        }
    }


    override fun getInputStream() : InputStream {
        val channel = openSFTPChannel()

        return RemoteInputStream(channel.get(unixPath), channel)
    }

    override fun getOutputStream() : OutputStream {
        val channel = openSFTPChannel()

        return RemoteOutputStream(channel.put(unixPath), channel)
    }


    fun openSFTPChannel() : ChannelSftp {
        val channel = session?.openChannel("sftp") as ChannelSftp
        channel.connect()

        return channel
    }

}


class RemoteInputStream(private val source: InputStream, private val channel: ChannelSftp) : InputStream() {

    @Throws(IOException::class)
    override fun read(): Int {
        return source.read()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        return source.read(b)
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return source.read(b, off, len)
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        return source.skip(n)
    }

    @Throws(IOException::class)
    override fun available(): Int {
        return source.available()
    }

    @Throws(IOException::class)
    override fun close() {
        source.close()
        channel.exit()
    }

    override fun mark(readlimit: Int) {
        source.mark(readlimit)
    }

    @Throws(IOException::class)
    override fun reset() {
        source.reset()
    }

    override fun markSupported(): Boolean {
        return source.markSupported()
    }

}


class RemoteOutputStream(private val target: OutputStream, private val channel: ChannelSftp) : OutputStream() {

    @Throws(IOException::class)
    override fun write(b: Int) {
        target.write(b)
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray) {
        target.write(b)
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        target.write(b, off, len)
    }

    @Throws(IOException::class)
    override fun flush() {
        target.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        target.close()
        channel.exit()
    }
}
