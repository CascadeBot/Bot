@file:JvmName("Main")
package org.cascadebot.bot

import dev.minn.jda.ktx.util.SLF4J

val logger by SLF4J("Main")

fun main() {
    println("Hello World!")
    println(config)
    logger.info("WOO")
}