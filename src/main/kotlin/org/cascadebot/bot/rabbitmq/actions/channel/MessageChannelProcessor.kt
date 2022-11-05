package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import dev.minn.jda.ktx.messages.MessageCreateBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.cascadebot.bot.Main
import org.cascadebot.bot.MessageType
import org.cascadebot.bot.rabbitmq.actions.Processor
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.EmbedData
import org.cascadebot.bot.rabbitmq.objects.MessageData
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import org.cascadebot.bot.utils.PaginationUtil

class MessageChannelProcessor : Processor {

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

        val channel = ChannelUtils.validateAndGetChannel(body, guild)

        if (channel == null) {
            return CommonResponses.CHANNEL_NOT_FOUND
        }

        channel as MessageChannel

        // TODO I'm keeping this around for any basic actions we might have, maybe remove if this isn't used
        /*if (parts.isEmpty()) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            )
        }*/

        if (parts.size <= 1) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        when (parts[0]) {
            "send" -> {
                when (parts[1]) {
                    // channel:message:send:simple
                    "simple" -> {
                        channel.sendMessage(MessageCreateBuilder {
                            embeds += MessageType.INFO.embed.apply {
                                description = body.get("message").asText()
                            }.build()
                        }.build()).queue(
                            {
                                RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
                            },
                            ErrorHandler.handleError(envelope, properties, rabbitMqChannel)
                        )
                        return null
                    }
                    // channel:message:send:complex
                    "complex" -> {
                        val builder = MessageCreateBuilder()
                        val message = body.get("message")
                        if (message.has("embeds")) {
                            for (embedObj in message.get("embeds")) {
                                val embed = Main.json.treeToValue(embedObj, EmbedData::class.java)
                                builder.addEmbeds(embed.messageEmbed)
                            }
                        }
                        if (message.has("content")) {
                            builder.setContent(message.get("content").asText())
                        }

                        channel.sendMessage(builder.build()).queue({
                            RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
                        }, ErrorHandler.handleError(envelope, properties, rabbitMqChannel))
                        return null
                    }
                }
            }

            "messages" -> {
                when (parts[1]) {
                    // channel:message:messages:list
                    "list" -> {
                        val params = PaginationUtil.parsePaginationParameters(body)
                        channel.getHistoryBefore(params.start, params.count).queue({ history ->
                            RabbitMQResponse.success(history
                                .retrievedHistory.map { MessageData.fromMessage(it) })
                                .sendAndAck(rabbitMqChannel, properties, envelope)
                        }, ErrorHandler.handleError(envelope, properties, rabbitMqChannel))
                        return null
                    }
                    // channel:message:messages:id
                    "id" -> {
                        val messageId = body.get("message_id").asLong()
                        channel.retrieveMessageById(messageId).queue(
                            {
                                RabbitMQResponse.success(MessageData.fromMessage(it))
                                    .sendAndAck(rabbitMqChannel, properties, envelope)
                            },
                            ErrorHandler.handleError(envelope, properties, rabbitMqChannel)
                        )
                    }
                }
            }
        }

        return CommonResponses.UNSUPPORTED_ACTION
    }
}