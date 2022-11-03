package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.actions.Processor
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.MemberResponse
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import org.cascadebot.bot.utils.PaginationUtil

class TextChannelProcessor : Processor {

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

        if (parts.size <= 1) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        channel as TextChannel

        when (parts[0]) {
            "topic" -> {
                when (parts[1]) {
                    // channel:text:topic:get
                    "get" -> {
                        return RabbitMQResponse.success("topic", channel.topic)
                    }
                    // channel:text:topic:set
                    "set" -> {
                        val old = channel.topic
                        val newVal = body.get("topic").asText()
                        channel.manager.setTopic(newVal).queue({
                            val node = Main.json.createObjectNode()
                            node.put("old_topic", old)
                            node.put("new_topic", newVal)
                            RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
                        }, {
                            ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                        })
                        return null
                    }
                }
            }

            "users" -> {
                if (parts[1] == "list") {
                    val params = PaginationUtil.parsePaginationParameters(body)
                    return RabbitMQResponse.success(params.paginate(channel.members.map { MemberResponse.fromMember(it) }))
                }
            }
        }

        return CommonResponses.UNSUPPORTED_ACTION
    }
}