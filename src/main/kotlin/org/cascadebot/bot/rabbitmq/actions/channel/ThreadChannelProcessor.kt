package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import org.cascadebot.bot.rabbitmq.actions.Processor
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse

class ThreadChannelProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        shard: JDA
    ): RabbitMQResponse<*> {
        val guildId = body.get("guild_id").asLong()

        val guild = shard.getGuildById(guildId)!! // Shard Consumer runs checks, so should not be null

        val channel = ChannelUtils.validateAndGetChannel(body, guild)

        if (channel == null) {
            return CommonResponses.CHANNEL_NOT_FOUND
        }

        channel as ThreadChannel

        // TODO there isn't much onm thread channel that isn't covered else where.

        return CommonResponses.UNSUPPORTED_ACTION
    }
}