package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.Permission
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.RoleMoved
import org.cascadebot.bot.rabbitmq.objects.RolePermission
import org.cascadebot.bot.rabbitmq.objects.RoleResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler

class RoleConsumer : ActionConsumer {
    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        channel: Channel,
        shard: Int
    ) {
        if (parts.size <= 1) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidAction,
                "The specified action is not supported"
            ).sendAndAck(channel, properties, envelope)
            return
        }

        val roleId = body.get("role").get("id").asLong()
        val guildId = body.get("role").get("guild").asLong()

        val guild = Main.shardManager.getShardById(shard)?.getGuildById(guildId)
        val role = guild?.getRoleById(roleId)

        if (role == null) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidRole,
                "The specified role was not found"
            ).sendAndAck(channel, properties, envelope)
            return
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
                        RabbitMQResponse.success(perms).sendAndAck(channel, properties, envelope)
                        return
                    }
                    // role:permissions:set
                    "set" -> {
                        val permNode = body.get("perm")
                        val perm = Permission.valueOf(permNode.get("permission").asText().uppercase())
                        val state = permNode.get("state").asBoolean()
                        if (state) {
                            role.manager.givePermissions(perm).queue({
                                RabbitMQResponse.success().sendAndAck(channel, properties, envelope)
                            }, {
                                ErrorHandler.handleError(envelope, properties, channel, it)
                            })
                        } else {
                            role.manager.revokePermissions(perm).queue({
                                RabbitMQResponse.success().sendAndAck(channel, properties, envelope)
                            }, {
                                ErrorHandler.handleError(envelope, properties, channel, it)
                            })
                        }
                        return
                    }
                }
            }
            "position" -> {
                // role:position:set
                if (parts[1] == "set") {
                    val pos = body.get("position").asInt()
                    val current = role.position
                    guild.modifyRolePositions().selectPosition(role).moveTo(pos).queue({
                        RabbitMQResponse.success(RoleMoved(current, pos)).sendAndAck(channel, properties, envelope)
                    },
                    {
                        if (it is IllegalArgumentException) {
                            RabbitMQResponse.failure(
                                StatusCode.BadRequest,
                                InvalidErrorCodes.InvalidPosition,
                                "The specified position is out of bounds!"
                            )
                        } else {
                            ErrorHandler.handleError(envelope, properties, channel, it)
                        }
                    })
                    return
                }
            }
            "tags" -> {
                // role:tags:get
                if (parts[1] == "get") {
                    RabbitMQResponse.success(role.tags).sendAndAck(channel, properties, envelope)
                    return
                }
            }
        }

        RabbitMQResponse.failure(
            StatusCode.BadRequest,
            InvalidErrorCodes.InvalidAction,
            "The specified action is not supported"
        ).sendAndAck(channel, properties, envelope)
    }
}