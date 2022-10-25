package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.internal.interactions.DeferrableInteractionImpl
import net.dv8tion.jda.internal.interactions.InteractionHookImpl
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.actions.channel.ChannelUtils
import org.cascadebot.bot.rabbitmq.objects.InteractionData
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MessageData
import org.cascadebot.bot.rabbitmq.objects.MiscErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler

class InteractionConsumer : ActionConsumer {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        shard: JDA
    ): RabbitMQResponse<*>? {
        if (parts.isEmpty()) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            ).sendAndAck(rabbitMqChannel, properties, envelope)
        }

        val guildId = body.get("guild_id").asLong()

        val guild = shard.getGuildById(guildId)!! // Shard Consumer runs checks, so should not be null

        val rmqInteraction = Main.json.treeToValue(body, InteractionData::class.java)

        val channel = ChannelUtils.validateAndGetChannel(body, guild)

        if (channel == null) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidChannel,
                "The specified channel was not found"
            )
        }

        if (channel !is MessageChannel) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidChannel,
                "Channel is not a message channel!"
            )
        }

        return RabbitMQResponse.failure(
            StatusCode.BadRequest,
            InvalidErrorCodes.InvalidAction,
            "The specified action is not supported"
        )
    }
}