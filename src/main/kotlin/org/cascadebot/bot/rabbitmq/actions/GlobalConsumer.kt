package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel
import org.cascadebot.bot.rabbitmq.objects.ChannelResponse
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MemberResponse
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.RoleResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.utils.PaginationUtil

class GlobalConsumer : ActionConsumer {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        shard: JDA
    ): RabbitMQResponse<*>? {
        if (parts.isEmpty()) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            )
        }

        if (parts[0] == "test") {
            // This is an example of a request to global that does not have further parts to it. Currently not used
            return null
        }

        if (parts.size <= 1) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            )
        }

        val guildId = body.get("guild_id").asLong()
        val guild = shard.getGuildById(guildId)!! // Shard Consumer runs checks, so should not be null

        when (parts[0]) {
            "user" -> {
                when (parts[1]) {
                    // global:user:byId
                    "byId" -> {
                        val userId = body.get("user").asLong()
                        val member =
                            guild.getMemberById(userId)

                        if (member == null) {
                            return RabbitMQResponse.failure(
                                StatusCode.BadRequest,
                                InvalidErrorCodes.InvalidUser,
                                "User was not found"
                            )
                        }

                        return RabbitMQResponse.success(
                            MemberResponse.fromMember(member)
                        )
                    }
                    // global:user:byName
                    "byName" -> {
                        val name = body.get("name").asText()
                        if (name == null) {
                            return RabbitMQResponse.failure(
                                StatusCode.BadRequest,
                                InvalidErrorCodes.InvalidName,
                                "Name was not found"
                            )
                        }

                        val members = guild.members
                            .filter {
                                (it.nickname?.startsWith(name, true) ?: false) || (it.user.name.startsWith(name, true))
                            }

                        val params = PaginationUtil.parsePaginationParameters(body)
                        val response = params.paginate(members.map { MemberResponse.fromMember(it) })
                        return RabbitMQResponse.success(response)
                    }
                }
            }

            "role" -> {
                when (parts[1]) {
                    // global:role:byId
                    "byId" -> {
                        val roleId = body.get("role").asLong()
                        val role =
                            guild.getRoleById(roleId)

                        if (role == null) {
                            return RabbitMQResponse.failure(
                                StatusCode.BadRequest,
                                InvalidErrorCodes.InvalidRole,
                                "Role was not found"
                            )
                        }

                        return RabbitMQResponse.success(
                            RoleResponse.fromRole(role)
                        )
                    }
                    // global:role:byName
                    "byName" -> {
                        val name = body.get("name").asText()
                        if (name == null) {
                            return RabbitMQResponse.failure(
                                StatusCode.BadRequest,
                                InvalidErrorCodes.InvalidName,
                                "Name was not found"
                            )
                        }

                        val roles = guild.getRolesByName(name, true)

                        val params = PaginationUtil.parsePaginationParameters(body)
                        val response = params.paginate(roles.map { RoleResponse.fromRole(it) })
                        return RabbitMQResponse.success(response)
                    }
                }
            }

            "channel" -> {
                when (parts[1]) {
                    // global:channel:byId
                    "byId" -> {
                        val channelId = body.get("channel").asLong()
                        val discordChannel =
                            guild.getGuildChannelById(channelId)

                        if (discordChannel == null) {
                            return RabbitMQResponse.failure(
                                StatusCode.BadRequest,
                                InvalidErrorCodes.InvalidChannel,
                                "Channel was not found"
                            )
                        }

                        return RabbitMQResponse.success(
                            ChannelResponse.fromChannel(discordChannel as StandardGuildChannel)
                        )
                    }
                    // global:channel:byName
                    "byName" -> {
                        val name = body.get("name").asText()
                        if (name == null) {
                            return RabbitMQResponse.failure(
                                StatusCode.BadRequest,
                                InvalidErrorCodes.InvalidName,
                                "Name was not found"
                            )
                        }

                        val channels = guild.channels.filter { it.name.equals(name, true) }

                        val params = PaginationUtil.parsePaginationParameters(body)
                        val response =
                            params.paginate(channels.map { ChannelResponse.fromChannel(it as StandardGuildChannel) })
                        return RabbitMQResponse.success(response)
                    }
                }
            }
        }

        // If it gets to this point, action was not found
        return RabbitMQResponse.failure(
            StatusCode.BadRequest,
            InvalidErrorCodes.InvalidAction,
            "The specified action is not supported"
        )
    }
}