package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import org.cascadebot.bot.rabbitmq.actions.Processor
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MemberResponse
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import org.cascadebot.bot.utils.PaginationUtil

class VoiceChanelProcessor : Processor {

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

        channel as AudioChannel

        when (parts[0]) {
            "list" -> {
                if (parts[1] == "user") {
                    val params = PaginationUtil.parsePaginationParameters(body)
                    return RabbitMQResponse.success(params.paginate(channel.members.map { MemberResponse.fromMember(it) }))
                }
            }

            "user" -> {
                val userId = body.get("user_id").asLong()
                val member = guild.getMemberById(userId)

                if (member == null) {
                    return RabbitMQResponse.failure(
                        StatusCode.BadRequest,
                        InvalidErrorCodes.InvalidUser,
                        "The specified member was not found"
                    )
                }

                if (member.voiceState == null) {
                    return RabbitMQResponse.failure(
                        StatusCode.BadRequest,
                        InvalidErrorCodes.InvalidUser,
                        "The specified member is not connected to any voice channels"
                    )
                }

                when (parts[1]) {
                    "move" -> {
                        guild.moveVoiceMember(member, channel).queue({
                            RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
                        },
                            {
                                ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                            })
                        return null
                    }
                }
            }
        }

        return CommonResponses.UNSUPPORTED_ACTION
    }
}