package de.derteufelqwe.driver

import java.io.File
import java.util.zip.Adler32
import kotlin.math.min

object Utils {

    private var BUFFER_READ_SIZE = 1024 * 1024 * 50L     // 50 MB

    fun hashFile(file: File): Long {
        val adler = Adler32()

        val input = file.inputStream()
        val buffer = ByteArray(min(BUFFER_READ_SIZE, file.length()).toInt())
        var readCount = input.read(buffer)

        while (readCount > 0) {
            adler.update(buffer, 0, readCount)
            readCount = input.read(buffer)
        }

        adler.update(file.readBytes())

        return adler.value
    }

}
