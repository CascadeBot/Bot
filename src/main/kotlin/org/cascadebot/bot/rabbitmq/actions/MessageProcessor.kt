package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.actions.channel.ChannelUtils
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MessageData
import org.cascadebot.bot.rabbitmq.objects.MiscErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler

class MessageProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        context: RabbitMQContext,
        guild: Guild
    ): RabbitMQResponse<*>? {
        if (parts.isEmpty()) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        val rmqMessage = Main.json.treeToValue<MessageData>(body)

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
                checkAction(parts, "edit") -> editMessage(it, rmqMessage, context)
                else -> CommonResponses.UNSUPPORTED_ACTION.sendAndAck(context)
            }
        }, ErrorHandler.handleError(context))

        return null
    }

    private fun editMessage(
        message: Message,
        rmqMessage: MessageData,
        context: RabbitMQContext
    ) {
        message.editMessage(rmqMessage.messageEditData).queue({
            RabbitMQResponse.success().sendAndAck(context)
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

                    else -> ErrorHandler.handleError(context, error)
                }
            })
    }
}