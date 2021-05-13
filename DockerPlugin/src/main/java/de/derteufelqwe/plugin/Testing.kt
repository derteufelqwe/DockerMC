package de.derteufelqwe.plugin

import de.derteufelqwe.commons.hibernate.SessionBuilder
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFile
import de.derteufelqwe.commons.hibernate.objects.volumes.VolumeFolder
import org.hibernate.Session
import java.io.File
import java.util.zip.Adler32
import javax.persistence.NoResultException
import kotlin.jvm.Throws
import kotlin.math.min
import kotlin.system.measureTimeMillis



/*
 * 400 Iterations:
 *  Big session, default query: 15000, 15000, 15000
 *  Small session, default query: 4800, 4900, 4800
 *  Single query using in (300 files): 12000, 13000, 13000
 *  Big session, query only id: 317, 311, 322
 *  Small session, query only id: 700, 700, 700
 *  Query IDs, then query the objects by id: 2500
 *  Query everything at once, lazily fetch data: 3300
 *
 * Notes:
 *  - Changing querying for volume ID instead of volume doesn't change anything
 */


fun main() {
//    testDBPerformance()
    testHashPerformance()
//    testDB()
}


fun testDBPerformance() {
    val sessionBuilder = SessionBuilder("admin")

    val ids = generateSequence(317L) {
        it + 1L
    }.take(92).toList()

    val tStart = System.currentTimeMillis()
    sessionBuilder.execute { session ->
        for (id in ids) {
            val vol = session.get(VolumeFolder::class.java, id)
            val files = getVolumeFiles(session, vol)
            val a = 0;
        }
    }
    val tEnd = System.currentTimeMillis()


    println("Took ${tEnd - tStart}ms")
}

fun testHashPerformance() {
    val ITER = 100
    val PATH = "C:/Users/Arne/Downloads/hotswap-agent-1.4.1.jar"


    var file = File(PATH)
    var time1 = 0L
    var time2 = 0L

    time1 = measureTimeMillis {
        for (i in 0..ITER) {
            hash1(file)
        }
    }

    time2 = measureTimeMillis {
        for (i in 0..ITER) {
            hash2(file)
        }
    }

    println("Hash1 took $time1 ms")
    println("Hash2 took $time2 ms")
}

fun hash1(file: File): Any {
    val adler = Adler32()
    val input = file.inputStream()
    val buffer = ByteArray(min(1024 * 1024 * 50, file.length()).toInt())
    var readCount = input.read(buffer)

    while (readCount > 0) {
        adler.update(buffer, 0, readCount)
        readCount = input.read(buffer)
    }

    return adler.value
}

fun hash2(file: File): Any {
    val adler = Adler32()
    adler.update(file.readBytes())

    return adler.value
}



@Throws(NoResultException::class)
fun getVolumeFiles(session: Session, parent: VolumeFolder): List<VolumeFile> {
    // language=HQL
    val query = """
        SELECT 
            f
        FROM 
            VolumeFile AS f
        WHERE 
            f.parent = :parentFolder
    """.trimIndent()

    val result = session.createQuery(query, VolumeFile::class.java)
        .setParameter("parentFolder", parent)
        .resultList

    return result
}


@Throws(NoResultException::class)
fun getVolumeFolders(session: Session, parent: VolumeFolder): Map<String, VolumeFolder> {
    // language=HQL
    val query = """
            SELECT 
                f
            FROM 
                VolumeFolder AS f
            WHERE 
                f.parent = :parentFolder
        """.trimIndent()

    val result = session.createQuery(query, VolumeFolder::class.java)
        .setParameter("parentFolder", parent)
        .resultList

    return result
        .associateBy { it.name }
}
