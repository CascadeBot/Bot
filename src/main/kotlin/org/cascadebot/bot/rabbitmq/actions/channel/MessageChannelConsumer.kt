package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import dev.minn.jda.ktx.messages.EmbedBuilder
import dev.minn.jda.ktx.messages.InlineEmbed
import dev.minn.jda.ktx.messages.MessageCreateBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.cascadebot.bot.Main
import org.cascadebot.bot.MessageType
import org.cascadebot.bot.rabbitmq.actions.ActionConsumer
import org.cascadebot.bot.rabbitmq.objects.Embed
import org.cascadebot.bot.rabbitmq.objects.EmbedFooter
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import org.cascadebot.bot.utils.PaginationUtil

class MessageChannelConsumer : ActionConsumer {
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
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidChannel,
                "The specified channel was not found"
            )
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
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            )
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
                        }.build()).queue({
                            RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
                        },
                            {
                                ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                            })
                        return null
                    }
                    // channel:message:send:complex
                    "complex" -> {
                        val builder = MessageCreateBuilder()
                        val message = body.get("message")
                        if (message.has("embeds")) {
                            for (embedObj in message.get("embeds")) {
                                val embed = Main.json.treeToValue(embedObj, Embed::class.java)
                                builder.addEmbeds(EmbedBuilder {
                                    title = embed.title
                                    description = embed.description
                                    url = embed.url
                                    color = embed.color?.rgb
                                    timestamp = embed.timestamp
                                    if (embed.footer != null) {
                                        footer {
                                            name = embed.footer.text
                                            iconUrl = embed.footer.iconUrl
                                        }
                                    }
                                    image = embed.image
                                    thumbnail = embed.thumbnail
                                    if (embed.author != null) {
                                        author {
                                            name = embed.author.name
                                            url = embed.author.url
                                            iconUrl = embed.author.iconUrl
                                        }
                                    }
                                }.build())
                            }
                        }
                        if (message.has("content")) {
                            builder.setContent(message.get("content").asText())
                        }

                        channel.sendMessage(builder.build()).queue({
                            RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
                        }, {
                            ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                        })
                        return null
                    }
                }
            }

            "messages" -> {
                when (parts[1]) {
                    // channel:message:messages:list
                    "list" -> {
                        val params = PaginationUtil.parsePaginationParameters(body)
                        channel.getHistoryBefore(params.start, params.count).queue({
                            RabbitMQResponse.success(it.retrievedHistory)
                                .sendAndAck(rabbitMqChannel, properties, envelope)
                        }, {
                            ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                        })
                        return null
                    }
                    // channel:message:messages:find
                    "find" -> {
                        TODO("Need to figure out how this would be best handled")
                    }
                }
            }
        }

        return RabbitMQResponse.failure(
            StatusCode.BadRequest,
            InvalidErrorCodes.InvalidAction,
            "The specified action is not supported"
        )
    }
}