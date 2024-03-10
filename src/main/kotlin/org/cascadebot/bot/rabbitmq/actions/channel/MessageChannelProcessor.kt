package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import dev.minn.jda.ktx.messages.MessageCreateBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.cascadebot.bot.Main
import org.cascadebot.bot.MessageType
import org.cascadebot.bot.rabbitmq.actions.Processor
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.EmbedData
import org.cascadebot.bot.rabbitmq.objects.MessageData
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import org.cascadebot.bot.utils.PaginationUtil

class MessageChannelProcessor : Processor {

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

        channel as MessageChannel

        return when {
            checkAction(parts, "send", "simple") -> sendSimpleMessage(
                channel,
                body,
                context,
            )

            checkAction(parts, "send", "complex") -> sendComplexMessage(
                body,
                channel,
                context,
            )

            checkAction(parts, "list") -> listMessages(body, channel, context)
            checkAction(parts, "byId") -> getMessageById(body, channel, context)
            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun getMessageById(
        body: ObjectNode,
        channel: MessageChannel,
        context: RabbitMQContext,
    ): Nothing? {
        val messageId = body.get("message_id").asLong()
        channel.retrieveMessageById(messageId).queue(
            {
                RabbitMQResponse.success(MessageData.fromMessage(it))
                    .sendAndAck(context)
            },
            ErrorHandler.handleError(context)
        )
        return null
    }

    private fun listMessages(
        body: ObjectNode,
        channel: MessageChannel,
        context: RabbitMQContext,
    ): Nothing? {
        val params = PaginationUtil.parsePaginationParameters(body)
        channel.getHistoryBefore(params.start, params.count).queue({ history ->
            RabbitMQResponse.success(history
                .retrievedHistory.map { MessageData.fromMessage(it) })
                .sendAndAck(context)
        }, ErrorHandler.handleError(context))
        return null
    }

    private fun sendComplexMessage(
        body: ObjectNode,
        channel: MessageChannel,
        context: RabbitMQContext,
    ): Nothing? {
        val builder = MessageCreateBuilder()
        val message = body.get("message")
        if (message.has("embeds")) {
            for (embedObj in message.get("embeds")) {
                val embed = Main.json.treeToValue<EmbedData>(embedObj)
                builder.addEmbeds(embed.messageEmbed)
            }
        }
        if (message.has("content")) {
            builder.setContent(message.get("content").asText())
        }

        channel.sendMessage(builder.build()).queue({
            RabbitMQResponse.success().sendAndAck(context)
        }, ErrorHandler.handleError(context))
        return null
    }

    private fun sendSimpleMessage(
        channel: MessageChannel,
        body: ObjectNode,
        context: RabbitMQContext,
    ): Nothing? {
        channel.sendMessage(MessageCreateBuilder {
            embeds += MessageType.INFO.embed.apply {
                description = body.get("message").asText()
            }.build()
        }.build()).queue(
            {
                RabbitMQResponse.success().sendAndAck(context)
            },
            ErrorHandler.handleError(context)
        )
        return null
    }
}