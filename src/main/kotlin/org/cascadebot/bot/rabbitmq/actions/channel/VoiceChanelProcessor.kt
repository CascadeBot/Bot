package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.entities.Guild
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
        guild: Guild
    ): RabbitMQResponse<*>? {
        val channel = ChannelUtils.validateAndGetChannel(body, guild)

        if (channel == null) {
            return CommonResponses.CHANNEL_NOT_FOUND
        }

        if (parts.size <= 1) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        channel as AudioChannel

        return when {
            checkAction(parts, "members", "list") -> listChannelMembers(body, channel)
            checkAction(parts, "member") -> {
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

                when  {
                    checkAction(parts, "member", "move") -> {
                        guild.moveVoiceMember(member, channel).queue(
                            {
                                RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
                            },
                            ErrorHandler.handleError(envelope, properties, rabbitMqChannel)
                        )
                        return null
                    }
                    else -> CommonResponses.UNSUPPORTED_ACTION
                }
            }
            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun listChannelMembers(
        body: ObjectNode,
        channel: AudioChannel
    ): RabbitMQResponse<PaginationUtil.PaginationResult<MemberResponse>> {
        val params = PaginationUtil.parsePaginationParameters(body)
        return RabbitMQResponse.success(params.paginate(channel.members.map { MemberResponse.fromMember(it) }))
    }
}