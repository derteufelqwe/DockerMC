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



fun getFiles(): List<String> {
    val names = """
        DSC_0199.JPG
        DSC_0001.JPG
        DSC_0008.JPG
        DSC_0325.JPG
        DSC_0326.JPG
        DSC_0297.JPG
        DSC_0131.JPG
        DSC_0039.JPG
        DSC_0162.JPG
        DSC_0098.JPG
        DSC_0232.JPG
        DSC_0097.JPG
        DSC_0120.JPG
        DSC_9984.JPG
        DSC_0244.JPG
        DSC_9990.JPG
        DSC_0006.JPG
        DSC_0156.JPG
        DSC_0286.JPG
        DSC_0264.JPG
        DSC_0062.JPG
        DSC_0231.JPG
        DSC_0067.JPG
        DSC_0215.JPG
        DSC_9995.JPG
        DSC_0048.JPG
        DSC_0060.JPG
        DSC_0143.JPG
        DSC_9985.JPG
        DSC_0246.JPG
        DSC_0183.JPG
        DSC_9987.JPG
        DSC_9983.JPG
        DSC_0114.JPG
        DSC_0344.JPG
        DSC_0024.JPG
        DSC_0168.JPG
        DSC_0176.JPG
        DSC_0163.JPG
        DSC_0106.JPG
        DSC_0333.JPG
        DSC_0036.JPG
        DSC_0300.JPG
        DSC_0149.JPG
        DSC_0123.JPG
        DSC_0173.JPG
        DSC_9988.JPG
        DSC_0127.JPG
        DSC_0150.JPG
        DSC_0288.JPG
        DSC_0341.JPG
        DSC_0253.JPG
        DSC_0332.JPG
        DSC_0138.JPG
        DSC_0349.JPG
        DSC_0154.JPG
        DSC_0169.JPG
        DSC_0343.JPG
        DSC_0155.JPG
        DSC_0221.JPG
        DSC_0111.JPG
        DSC_0137.JPG
        DSC_0110.JPG
        DSC_9994.JPG
        DSC_0257.JPG
        DSC_0192.JPG
        DSC_0077.JPG
        DSC_0218.JPG
        DSC_0294.JPG
        DSC_0081.JPG
        DSC_0240.JPG
        DSC_0151.JPG
        DSC_0348.JPG
        DSC_0290.JPG
        DSC_0324.JPG
        DSC_0178.JPG
        DSC_0339.JPG
        DSC_0122.JPG
        DSC_0167.JPG
        DSC_0194.JPG
        DSC_0139.JPG
        DSC_0237.JPG
        DSC_0335.JPG
        DSC_0141.JPG
        DSC_0291.JPG
        DSC_0107.JPG
        DSC_0217.JPG
        DSC_0342.JPG
        DSC_0180.JPG
        DSC_0044.JPG
        DSC_0132.JPG
        DSC_0235.JPG
        DSC_0338.JPG
        DSC_0219.JPG
        DSC_9992.JPG
        DSC_0265.JPG
        DSC_0116.JPG
        DSC_0250.JPG
        DSC_0209.JPG
        DSC_0252.JPG
        DSC_0226.JPG
        DSC_0185.JPG
        DSC_0090.JPG
        DSC_0202.JPG
        DSC_0117.JPG
        DSC_0034.JPG
        DSC_0142.JPG
        DSC_0057.JPG
        DSC_0242.JPG
        DSC_0337.JPG
        DSC_0243.JPG
        DSC_0181.JPG
        DSC_0148.JPG
        DSC_0287.JPG
        DSC_0211.JPG
        DSC_0172.JPG
        DSC_0063.JPG
        DSC_0205.JPG
        DSC_0258.JPG
        DSC_0224.JPG
        DSC_0096.JPG
        DSC_0103.JPG
        DSC_0213.JPG
        DSC_0227.JPG
        DSC_9997.JPG
        DSC_0095.JPG
        DSC_0251.JPG
        DSC_0100.JPG
        DSC_0238.JPG
        DSC_0043.JPG
        DSC_0336.JPG
        DSC_0076.JPG
        DSC_0065.JPG
        DSC_0296.JPG
        DSC_0128.JPG
        DSC_0038.JPG
        DSC_0041.JPG
        DSC_0145.JPG
        DSC_9993.JPG
        DSC_0068.JPG
        DSC_0206.JPG
        DSC_0093.JPG
        DSC_0010.JPG
        DSC_0113.JPG
        DSC_0012.JPG
        DSC_0082.JPG
        DSC_9991.JPG
        DSC_0018.JPG
        DSC_0284.JPG
        DSC_0108.JPG
        DSC_0091.JPG
        DSC_0214.JPG
        DSC_0161.JPG
        DSC_0171.JPG
        DSC_0013.JPG
        DSC_0074.JPG
        DSC_0014.JPG
        DSC_0241.JPG
        DSC_9989.JPG
        DSC_0298.JPG
        DSC_0140.JPG
        DSC_0129.JPG
        DSC_0049.JPG
        DSC_0260.JPG
        DSC_0329.JPG
        DSC_0334.JPG
        DSC_0037.JPG
        DSC_0002.JPG
        DSC_0124.JPG
        DSC_0255.JPG
        DSC_0311.JPG
        DSC_0134.JPG
        DSC_0203.JPG
        DSC_0282.JPG
        DSC_0204.JPG
        DSC_0256.JPG
        DSC_0340.JPG
        DSC_0292.JPG
        DSC_0228.JPG
        DSC_0196.JPG
        DSC_0118.JPG
        DSC_0293.JPG
        DSC_0072.JPG
        DSC_0099.JPG
        DSC_0212.JPG
        DSC_0054.JPG
        DSC_0056.JPG
        DSC_0165.JPG
        DSC_0283.JPG
        DSC_0230.JPG
        DSC_0195.JPG
        DSC_0079.JPG
        DSC_0200.JPG
        DSC_0254.JPG
        DSC_0005.JPG
        DSC_0003.JPG
        DSC_0157.JPG
        DSC_0045.JPG
        DSC_0281.JPG
        DSC_0080.JPG
        DSC_0236.JPG
        DSC_0105.JPG
        DSC_0059.JPG
        DSC_0125.JPG
        DSC_0229.JPG
        DSC_0069.JPG
        DSC_9998.JPG
        DSC_0133.JPG
        DSC_0198.JPG
        DSC_0201.JPG
        DSC_0050.JPG
        DSC_0112.JPG
        DSC_0153.JPG
        DSC_0182.JPG
        DSC_0109.JPG
        DSC_0225.JPG
        DSC_0164.JPG
        DSC_9999.JPG
        DSC_0248.JPG
        DSC_9986.JPG
        DSC_0004.JPG
        DSC_0130.JPG
        DSC_0295.JPG
        DSC_0179.JPG
        DSC_0092.JPG
        DSC_0078.JPG
        DSC_0328.JPG
        DSC_0234.JPG
        DSC_0119.JPG
        DSC_0233.JPG
        DSC_9996.JPG
        DSC_0070.JPG
        DSC_0346.JPG
        DSC_0159.JPG
        DSC_0104.JPG
        DSC_0055.JPG
        DSC_0170.JPG
        DSC_0115.JPG
        DSC_0216.JPG
        DSC_0126.JPG
        DSC_0262.JPG
        DSC_0011.JPG
        DSC_0351.JPG
        DSC_0017.JPG
        DSC_0285.JPG
        DSC_0289.JPG
        DSC_0158.JPG
        DSC_0299.JPG
        DSC_0266.JPG
        DSC_0042.JPG
        DSC_0331.JPG
        DSC_0345.JPG
        DSC_0190.JPG
        DSC_0052.JPG
        DSC_0058.JPG
        DSC_0210.JPG
        DSC_0330.JPG
        DSC_0066.JPG
        DSC_0350.JPG
        DSC_0009.JPG
        DSC_0222.JPG
        DSC_0347.JPG
        DSC_0268.JPG
        DSC_0223.JPG
        DSC_0186.JPG
        DSC_0073.JPG
        DSC_0177.JPG
        DSC_0193.JPG
        DSC_0301.JPG
        DSC_0121.JPG
        DSC_0327.JPG
        DSC_0016.JPG
        DSC_0033.JPG
        DSC_0197.JPG
        DSC_0007.JPG
        DSC_0061.JPG
    """.trimIndent()

    return names.split("/n")
}


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
    sessionBuilder.execute {
        for (id in ids) {
            val vol = it.get(VolumeFolder::class.java, id)
            val files = getVolumeFiles(it, vol)
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
