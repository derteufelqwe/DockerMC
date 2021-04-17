package de.derteufelqwe.driver

import java.io.File
import java.util.zip.Adler32

object Utils {

    fun hashFile(file: File): Long {
//        val blake = ove.crypto.digest.Blake2b.Digest.newInstance()
//        blake.update(file.readBytes())
//
//        return blake.digest()

        val adler = Adler32()
        adler.update(file.readBytes())

        return adler.value
    }

}
