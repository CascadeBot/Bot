package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
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
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler

class InteractionProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        context: RabbitMQContext,
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
                context,
                rmqInteraction
            )

            checkAction(parts, "reply", "complex") -> replyComplex(
                interactionHook,
                body,
                context,
                rmqInteraction
            )

            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun replyComplex(
        interactionHook: InteractionHook,
        body: ObjectNode,
        context: RabbitMQContext,
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
            RabbitMQResponse.success().sendAndAck(context)
        }, ErrorHandler.handleError(context))

        // We can't reply to an interaction twice, so we should invalidate this after it's been used
        Main.interactionHookCache.invalidate(rmqInteraction.interactionId)
        return null
    }

    private fun replySimple(
        interactionHook: InteractionHook,
        body: ObjectNode,
        context: RabbitMQContext,
        rmqInteraction: InteractionData
    ): Nothing? {
        interactionHook.editOriginal(MessageEditBuilder {
            embeds += MessageType.INFO.embed.apply {
                description = body.get("message").asText()
            }.build()
        }.build()).queue(
            {
                RabbitMQResponse.success().sendAndAck(context)
            },
            ErrorHandler.handleError(context)
        )

        // We can't reply to an interaction twice, so we should invalidate this after it's been used
        Main.interactionHookCache.invalidate(rmqInteraction.interactionId)
        return null
    }
}