package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.actions.ActionConsumer
import org.cascadebot.bot.rabbitmq.objects.HolderType
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.PermissionOverridePermission
import org.cascadebot.bot.rabbitmq.objects.PermissionOverrideState
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.RabbitMqPermissionOverride
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import org.cascadebot.bot.utils.PaginationUtil
import kotlin.streams.toList

class GenericChannelConsumer : ActionConsumer {
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

        if (parts.size <= 1) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            )
        }

        when (parts[0]) {
            "name" -> {
                // channel:general:name:set
                if (parts[1] == "set") {
                    val old = channel.name
                    val newName = body.get("name").asText()
                    channel.manager.setName(newName).queue({
                        val node = Main.json.createObjectNode()
                        node.put("oldName", old)
                        node.put("newName", newName)
                        RabbitMQResponse.success(node).sendAndAck(rabbitMqChannel, properties, envelope)
                    }, {
                        ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                    })
                }
            }

            "permissions" -> {
                when (parts[1]) {
                    // channel:general:permissions:list
                    "list" -> {
                        val params = PaginationUtil.parsePaginationParameters(body)
                        return RabbitMQResponse.success(
                            params.paginate(
                                channel.permissionContainer.permissionOverrides.map { RabbitMqPermissionOverride.fromPermissionOverride(it) }
                            )
                        )
                    }
                    // channel:general:permissions:put
                    "put" -> {
                        val override =
                            Main.json.treeToValue(body.get("override"), RabbitMqPermissionOverride::class.java)
                        val holder = when (override.holderType) {
                            HolderType.ROLE -> {
                                val role = guild.getRoleById(override.holderId)
                                if (role == null) {
                                    return null
                                }
                                role
                            }

                            HolderType.USER -> {
                                val member = guild.getMemberById(override.holderId)
                                if (member == null) {
                                    return null
                                }
                                member
                            }
                        }
                        val cal = calcPermsOverrides(override.permissions)
                        channel.permissionContainer.manager.putPermissionOverride(holder, cal.allow, cal.deny).queue({
                            RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
                        },
                            {
                                ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                            })
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

    private fun calcPermsOverrides(overrides: List<PermissionOverridePermission>): CalculatedPermissionOverride {
        val allow: MutableList<Permission> = mutableListOf()
        val deny: MutableList<Permission> = mutableListOf()
        for (override in overrides) {
            when (override.state) {
                PermissionOverrideState.ALLOW -> {
                    allow.add(override.permission)
                }

                PermissionOverrideState.DENY -> {
                    deny.add(override.permission)
                }

                else -> {}
            }
        }
        return CalculatedPermissionOverride(Permission.getRaw(allow), Permission.getRaw(deny))
    }

    data class CalculatedPermissionOverride(val allow: Long, val deny: Long)
}