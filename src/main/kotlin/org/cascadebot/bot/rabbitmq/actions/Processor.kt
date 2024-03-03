package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import net.dv8tion.jda.api.entities.Guild
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.hibernate.Session

interface Processor {

    fun consume(
        parts: List<String>,
        body: ObjectNode,
        context: RabbitMQContext,
        guild: Guild
    ): RabbitMQResponse<*>?

    fun <T> dbTransaction(work: Session.()->T): T {
        return Main.postgres.transaction(work)
    }

    fun checkAction(parts: List<String>, vararg actionParts: String): Boolean {
        if (parts.size < actionParts.size) return false

        for ((index, s) in actionParts.withIndex()) {
            if (parts[index] != s) return false
        }

        return true
    }

}