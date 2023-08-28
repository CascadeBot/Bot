package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import dev.minn.jda.ktx.messages.MessageEditBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import org.cascadebot.bot.Main
import org.cascadebot.bot.MessageType
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.EmbedData
import org.cascadebot.bot.rabbitmq.objects.InteractionData
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler

class InteractionProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        guild: Guild
    ): RabbitMQResponse<*>? {
        if (parts.size <= 1) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        val rmqInteraction = Main.json.treeToValue(body, InteractionData::class.java)

        val interactionHook = Main.interactionHookCache.getIfPresent(rmqInteraction.interactionId)

        if (interactionHook == null) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidInteraction,
                "The specified interaction ID could not be found in the cache"
            )
        }

        return when {
            checkAction(parts, "reply", "simple") -> replySimple(
                interactionHook,
                body,
                rabbitMqChannel,
                properties,
                envelope,
                rmqInteraction
            )

            checkAction(parts, "reply", "complex") -> replyComplex(
                interactionHook,
                body,
                rabbitMqChannel,
                properties,
                envelope,
                rmqInteraction
            )

            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun replyComplex(
        interactionHook: InteractionHook,
        body: ObjectNode,
        rabbitMqChannel: Channel,
        properties: AMQP.BasicProperties,
        envelope: Envelope,
        rmqInteraction: InteractionData
    ): Nothing? {
        val builder = MessageEditBuilder()
        val message = body.get("message")
        if (message.has("embeds")) {
            builder.setEmbeds(message.get("embeds").map {
                Main.json.treeToValue(it, EmbedData::class.java).messageEmbed
            })
        }
        if (message.has("content")) {
            builder.setContent(message.get("content").asText())
        }

        interactionHook.editOriginal(builder.build()).queue({
            RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
        }, ErrorHandler.handleError(envelope, properties, rabbitMqChannel))

        // We can't reply to an interaction twice, so we should invalidate this after it's been used
        Main.interactionHookCache.invalidate(rmqInteraction.interactionId)
        return null
    }

    private fun replySimple(
        interactionHook: InteractionHook,
        body: ObjectNode,
        rabbitMqChannel: Channel,
        properties: AMQP.BasicProperties,
        envelope: Envelope,
        rmqInteraction: InteractionData
    ): Nothing? {
        interactionHook.editOriginal(MessageEditBuilder {
            embeds += MessageType.INFO.embed.apply {
                description = body.get("message").asText()
            }.build()
        }.build()).queue(
            {
                RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
            },
            ErrorHandler.handleError(envelope, properties, rabbitMqChannel)
        )

        // We can't reply to an interaction twice, so we should invalidate this after it's been used
        Main.interactionHookCache.invalidate(rmqInteraction.interactionId)
        return null
    }
}