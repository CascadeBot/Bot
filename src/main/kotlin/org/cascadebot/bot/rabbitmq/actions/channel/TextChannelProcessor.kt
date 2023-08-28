package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.cascadebot.bot.rabbitmq.actions.Processor
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.MemberResponse
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import org.cascadebot.bot.utils.PaginationUtil
import org.cascadebot.bot.utils.createJsonObject

class TextChannelProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        guild: Guild
    ): RabbitMQResponse<*>? {
        val channel = ChannelUtils.validateAndGetChannel(body, guild)

        if (channel == null) {
            return CommonResponses.CHANNEL_NOT_FOUND
        }

        channel as TextChannel

        return when {
            checkAction(parts, "topic", "get") -> RabbitMQResponse.success("topic", channel.topic)
            checkAction(parts, "topic", "set") -> setChannelTopic(channel, body, rabbitMqChannel, properties, envelope)
            checkAction(parts, "members", "list") -> listChannelMembers(body, channel)
            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun listChannelMembers(
        body: ObjectNode,
        channel: TextChannel
    ): RabbitMQResponse<PaginationUtil.PaginationResult<MemberResponse>> {
        val params = PaginationUtil.parsePaginationParameters(body)
        return RabbitMQResponse.success(params.paginate(channel.members.map { MemberResponse.fromMember(it) }))
    }

    private fun setChannelTopic(
        channel: TextChannel,
        body: ObjectNode,
        rabbitMqChannel: Channel,
        properties: AMQP.BasicProperties,
        envelope: Envelope
    ): Nothing? {
        val old = channel.topic
        val newVal = body.get("topic").asText()
        channel.manager.setTopic(newVal).queue({
            val node = createJsonObject(
                "old_topic" to old,
                "new_topic" to newVal
            )
            RabbitMQResponse.success(node).sendAndAck(rabbitMqChannel, properties, envelope)
        }, ErrorHandler.handleError(envelope, properties, rabbitMqChannel))
        return null
    }
}