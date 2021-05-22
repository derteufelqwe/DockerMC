package de.derteufelqwe.nodewatcher

import de.derteufelqwe.commons.Utils
import java.util.regex.Pattern

val RE_FIND_PART = Pattern.compile("^(.+)\\s+part\\s+(.+)", Pattern.MULTILINE)
val PATTERN_RE_FIND_USAGE = "(\\/dev\\/%s)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+\\d+%%\\s+(.+)"

fun main() {
    val lsblk = Utils.executeCommandOnHost(arrayOf("lsblk", "-io", "KNAME,TYPE,SIZE,MODEL"))
    val df = Utils.executeCommandOnHost(arrayOf("df"))

    val partitions = mutableListOf<Partition>()
    val m1 = RE_FIND_PART.matcher(lsblk)

    while (m1.find()) {
        partitions.add(Partition(m1.group(1), m1.group(2)))
    }

    val usages = mutableListOf<PartitionUsage>()

    for (part in partitions) {
        val pattern = Pattern.compile(String.format(PATTERN_RE_FIND_USAGE, part.name), Pattern.MULTILINE)
        val m = pattern.matcher(df)
        if (m.find()) {
            usages.add(
                PartitionUsage(
                    name = m.group(1),
                    max = m.group(2).toLong(),
                    used = m.group(3).toLong(),
                    available = m.group(4).toLong(),
                    mountPoint = m.group(5)
                )
            )
        }

    }

    println()
}

class Partition(val name: String, val size: String)

class PartitionUsage(val name: String, val max: Long, val used: Long, val available: Long, val mountPoint: String) {

    val usedPercent = 100.0 / max * used

}