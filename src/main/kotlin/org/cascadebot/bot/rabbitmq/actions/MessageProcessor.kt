package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.actions.channel.ChannelUtils
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MessageData
import org.cascadebot.bot.rabbitmq.objects.MiscErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler

class MessageProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        guild: Guild
    ): RabbitMQResponse<*>? {
        if (parts.isEmpty()) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        val rmqMessage = Main.json.treeToValue(body, MessageData::class.java)

        val channel = ChannelUtils.validateAndGetChannel(body, guild)

        if (channel == null) {
            return CommonResponses.CHANNEL_NOT_FOUND
        }

        if (channel !is MessageChannel) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidChannel,
                "Channel is not a message channel!"
            )
        }

        channel.retrieveMessageById(rmqMessage.messageId).queue({
            when {
                checkAction(parts, "edit") -> editMessage(it, rmqMessage, rabbitMqChannel, properties, envelope)
                else -> CommonResponses.UNSUPPORTED_ACTION.sendAndAck(rabbitMqChannel, properties, envelope)
            }
        }, ErrorHandler.handleError(envelope, properties, rabbitMqChannel))

        return null
    }

    private fun editMessage(
        message: Message,
        rmqMessage: MessageData,
        rabbitMqChannel: Channel,
        properties: AMQP.BasicProperties,
        envelope: Envelope
    ) {
        message.editMessage(rmqMessage.messageEditData).queue({
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
                    }

                    else -> ErrorHandler.handleError(envelope, properties, rabbitMqChannel, error)
                }
            })
    }
}