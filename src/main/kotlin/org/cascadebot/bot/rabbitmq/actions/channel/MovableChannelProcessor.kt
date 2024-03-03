package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.attribute.IPositionableChannel
import org.cascadebot.bot.rabbitmq.actions.Processor
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import org.cascadebot.bot.utils.createJsonObject

class MovableChannelProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        context: RabbitMQContext,
        guild: Guild
    ): RabbitMQResponse<*>? {
        val channel = ChannelUtils.validateAndGetChannel(body, guild)

        if (channel == null) {
            return CommonResponses.CHANNEL_NOT_FOUND
        }

        channel as IPositionableChannel

        return when {
            checkAction(parts, "position", "set") -> setChannelPosition(
                channel,
                body,
                context,
            )

            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun setChannelPosition(
        channel: IPositionableChannel,
        body: ObjectNode,
        context: RabbitMQContext
    ): Nothing? {
        val old = channel.position
        val newPos = body.get("pos").asInt()
        channel.manager.setPosition(newPos).queue({
            val node = createJsonObject(
                "old_pos" to old,
                "new_pos" to newPos
            )
            RabbitMQResponse.success(node).sendAndAck(context)
        }, ErrorHandler.handleError(context))
        return null
    }
}