package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer
import org.cascadebot.bot.rabbitmq.actions.ActionConsumer
import org.cascadebot.bot.rabbitmq.objects.ChannelResponse
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.utils.PaginationUtil

class ChannelWithThreadsConsumer : ActionConsumer {

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
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidChannel,
                "The specified channel was not found"
            )
        }

        if (parts.isEmpty()) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            )
        }

        channel as IThreadContainer

        when (parts[0]) {
            // channel:threaded:list
            "list" -> {
                val params = PaginationUtil.parsePaginationParameters(body)
                return RabbitMQResponse.success(params.paginate(channel.threadChannels.map {
                    ChannelResponse.fromThread(
                        it
                    )
                }))
            }
        }

        return RabbitMQResponse.failure(
            StatusCode.BadRequest,
            InvalidErrorCodes.InvalidAction,
            "The specified action is not supported"
        )
    }
}