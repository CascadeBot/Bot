package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.actions.channel.ChannelUtils
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MiscErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.MessageResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import java.lang.IllegalStateException

class MessageConsumer : ActionConsumer {
    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        shard: JDA
    ): RabbitMQResponse<*>? {
        val guildId = body.get("guild_id").asLong()

        val guild = shard.getGuildById(guildId)!! // Shard Consumer runs checks, so should not be null

        val rmqMessage = Main.json.treeToValue(body, MessageResponse::class.java)

        val channel = ChannelUtils.validateAndGetChannel(body, guild)

        if (channel == null) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidChannel,
                "The specified channel was not found"
            )
        }

        channel as MessageChannel

        channel.retrieveMessageById(rmqMessage.messageId).queue({
            if (parts.isEmpty()) {
                RabbitMQResponse.failure(
                    StatusCode.BadRequest,
                    InvalidErrorCodes.InvalidAction,
                    "The specified action is not supported"
                ).sendAndAck(rabbitMqChannel, properties, envelope)
            }

            when (parts[0]) {
                // message:edit
                "edit" -> {
                    it.editMessage(rmqMessage.toDiscordEditMessage()).queue({
                        RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
                    },
                        { error ->
                            when (error) {
                                is IllegalStateException -> {
                                    RabbitMQResponse.failure(
                                        StatusCode.BadRequest,
                                        MiscErrorCodes.BotDoesNotOwnMessage,
                                        "The bot does not own the message trying to be edited"
                                    )
                                    return@queue
                                }
                            }
                            ErrorHandler.handleError(envelope, properties, rabbitMqChannel, error)
                        })
                }
            }

            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            ).sendAndAck(rabbitMqChannel, properties, envelope)
        }, {
            ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
        })

        return null
    }
}