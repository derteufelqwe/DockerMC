package de.derteufelqwe.commons

import java.lang.StringBuilder
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1


/**
 * Alternative to lomboks @ToString annotation for kotlin classes
 */
fun Any.reflectionToString(vararg exclude: String): String {
    val sb = StringBuilder(this::class.simpleName + "(")

    val properties = this::class.members
        .filter { it is KProperty }
        .filter { it.name !in exclude }

    for ((i, prop) in properties.withIndex()) {
        sb.append("${prop.name}=")
        sb.append((prop as KProperty1<Any, *>).get(this).toString())
        if (i + 1 < properties.size) {
            sb.append(", ")
        }
    }

    sb.append(")")
    return sb.toString()
}