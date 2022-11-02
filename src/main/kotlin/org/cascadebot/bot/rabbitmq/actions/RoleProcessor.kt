package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.RoleMoved
import org.cascadebot.bot.rabbitmq.objects.RolePermission
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler

class RoleProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        shard: JDA
    ): RabbitMQResponse<*>? {
        if (parts.size <= 1) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        val roleId = body.get("role_id").asLong()
        val guildId = body.get("guild_id").asLong()

        val guild = shard.getGuildById(guildId)!! // Shard Consumer runs checks, so should not be null
        val role = guild.getRoleById(roleId)

        if (role == null) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidRole,
                "The specified role was not found"
            )
        }

        when (parts[0]) {
            "permissions" -> {
                when (parts[1]) {
                    // role:permissions:list
                    "list" -> {
                        val perms = mutableListOf<RolePermission>()
                        for (perm in Permission.values()) {
                            perms.add(RolePermission(perm, role.hasPermission(perm)))
                        }
                        return RabbitMQResponse.success(perms)
                    }
                    // role:permissions:set
                    "set" -> {
                        val permNode = body.get("perm")
                        val perm = Permission.valueOf(permNode.get("permission").asText().uppercase())
                        val state = permNode.get("state").asBoolean()
                        if (state) {
                            role.manager.givePermissions(perm).queue({
                                RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
                            }, {
                                ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                            })
                        } else {
                            role.manager.revokePermissions(perm).queue({
                                RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
                            }, {
                                ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                            })
                        }
                        return null
                    }
                }
            }

            "position" -> {
                // role:position:set
                if (parts[1] == "set") {
                    val pos = body.get("position").asInt()
                    val current = role.position
                    guild.modifyRolePositions().selectPosition(role).moveTo(pos).queue(
                        {
                            RabbitMQResponse.success(RoleMoved(current, pos))
                                .sendAndAck(rabbitMqChannel, properties, envelope)
                        },
                        {
                            if (it is IllegalArgumentException) {
                                RabbitMQResponse.failure(
                                    StatusCode.BadRequest,
                                    InvalidErrorCodes.InvalidPosition,
                                    "The specified position is out of bounds!"
                                )
                            } else {
                                ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                            }
                        }
                    )
                    return null
                }
            }

            "tags" -> {
                // role:tags:get
                if (parts[1] == "get") {
                    return RabbitMQResponse.success(role.tags)
                }
            }
        }

        return CommonResponses.UNSUPPORTED_ACTION
    }
}