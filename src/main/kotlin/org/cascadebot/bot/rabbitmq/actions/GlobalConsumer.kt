package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.ChannelResponse
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.RoleResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.objects.MemberResponse

class GlobalConsumer : ActionConsumer {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        channel: Channel,
        shard: Int
    ) {
        if (parts.isEmpty()) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            ).sendAndAck(channel, properties, envelope)
            return
        }

        if (parts[0] == "test") {
            // This is an example of a request to global that does not have further parts to it. Currently not used
        }

        if (parts.size <= 1) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            ).sendAndAck(channel, properties, envelope)
            return
        }

        val guildId = body.get("guild").asLong()
        val shardId = ((guildId shr 22) % Main.shardManager.shardsTotal).toInt()
        val guild = Main.shardManager.getShardById(shardId)?.getGuildById(guildId)

        when (parts[0]) {
            "user" -> {
                when (parts[1]) {
                    // global:user:byId
                    "byId" -> {
                        val userId = body.get("user").asLong()
                        val member =
                            guild?.getMemberById(userId)

                        if (member == null) {
                            RabbitMQResponse.failure(
                                StatusCode.BadRequest,
                                InvalidErrorCodes.InvalidUser,
                                "User was not found"
                            ).sendAndAck(channel, properties, envelope)
                            return
                        }

                        RabbitMQResponse.success(
                            MemberResponse.fromMember(member)
                        )

                        return
                    }
                    // global:user:byName
                    "byName" -> {
                        // TODO pagination
                        return
                    }
                }
            }

            "role" -> {
                when (parts[1]) {
                    // global:role:byId
                    "byId" -> {
                        val roleId = body.get("role").asLong()
                        val role =
                            guild?.getRoleById(roleId)

                        if (role == null) {
                            RabbitMQResponse.failure(
                                StatusCode.BadRequest,
                                InvalidErrorCodes.InvalidRole,
                                "Role was not found"
                            ).sendAndAck(channel, properties, envelope)
                            return
                        }

                        RabbitMQResponse.success(
                            RoleResponse.fromRole(role)
                        )

                        return
                    }
                    // global:role:byName
                    "byName" -> {
                        // TODO pagination
                        return
                    }
                }
            }

            "channel" -> {
                when (parts[1]) {
                    // global:channel:byId
                    "byId" -> {
                        val channelId = body.get("channel").asLong()
                        val discordChannel =
                            guild?.getGuildChannelById(channelId)

                        if (discordChannel == null) {
                            RabbitMQResponse.failure(
                                StatusCode.BadRequest,
                                InvalidErrorCodes.InvalidChannel,
                                "Channel was not found"
                            ).sendAndAck(channel, properties, envelope)
                            return
                        }

                        RabbitMQResponse.success(
                            ChannelResponse.fromChannel(discordChannel as StandardGuildChannel)
                        )

                        return
                    }
                    // global:channel:byName
                    "byName" -> {
                        // TODO pagination
                        return
                    }
                }
            }
        }

        // If it gets to this point, action was not found
        RabbitMQResponse.failure(
            StatusCode.BadRequest,
            InvalidErrorCodes.InvalidAction,
            "The specified action is not supported"
        ).sendAndAck(channel, properties, envelope)
        return
    }
}