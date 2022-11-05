package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.attribute.IPositionableChannel
import org.cascadebot.bot.rabbitmq.actions.Processor
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import org.cascadebot.bot.utils.createJsonObject

class MovableChannelProcessor : Processor {

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

        channel as IPositionableChannel

        if (parts.size <= 1) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        when (parts[0]) {
            "position" -> {
                // channel:general:name:set
                if (parts[1] == "set") {
                    val old = channel.position
                    val newPos = body.get("pos").asInt()
                    channel.manager.setPosition(newPos).queue({
                        val node = createJsonObject(
                            "old_pos" to old,
                            "new_pos" to newPos
                        )
                        RabbitMQResponse.success(node).sendAndAck(rabbitMqChannel, properties, envelope)
                    }, ErrorHandler.handleError(envelope, properties, rabbitMqChannel))
                }
            }
        }

        return CommonResponses.UNSUPPORTED_ACTION
    }
}