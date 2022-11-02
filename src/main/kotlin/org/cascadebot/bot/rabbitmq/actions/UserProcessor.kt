package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.RoleResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import org.cascadebot.bot.utils.PaginationUtil
import java.awt.Color

class UserProcessor : Processor {

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

        val userId = body.get("user_id").asLong()
        val guildId = body.get("guild_id").asLong()

        val guild = shard.getGuildById(guildId)!! // Shard Consumer runs checks, so should not be null
        val member = guild.getMemberById(userId)

        if (member == null) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidUser,
                "The specified member was not found"
            )
        }

        when (parts[0]) {
            "list" -> {
                // TODO pagination
                when (parts[1]) {
                    // user:list:role
                    "role" -> {
                        val params = PaginationUtil.parsePaginationParameters(body)
                        val response = params.paginate(member.roles.map { RoleResponse.fromRole(it) })
                        return RabbitMQResponse.success(response)
                    }
                }
            }

            "color" -> {
                Color.BLACK.rgb
                // user:color:get
                if (parts[1] == "get") {
                    val node = Main.json.createObjectNode()
                    node.replace("color", Main.json.valueToTree(member.color))
                    return RabbitMQResponse.success(node)
                }
            }

            "permission" -> {
                val permission: Permission
                try {
                    permission = Permission.valueOf(body.get("permission").asText().uppercase())
                } catch (e: IllegalArgumentException) {
                    return RabbitMQResponse.failure(
                        StatusCode.BadRequest,
                        InvalidErrorCodes.InvalidPermission,
                        "The specified permission doesn't exist"
                    )
                }
                if (parts[1] == "has") {
                    val node = Main.json.createObjectNode()
                    node.put("hasPerm", member.hasPermission(permission))
                    return RabbitMQResponse.success(node)
                }
            }

            "nick" -> {
                val nick = body.get("nick").asText()
                // user:nick:set
                if (parts[1] == "set") {
                    member.modifyNickname(nick).queue({
                        RabbitMQResponse.success()
                            .sendAndAck(rabbitMqChannel, properties, envelope)
                    }, {
                        ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                    })
                    return null
                }
            }

            "role" -> {
                val roleId = body.get("role").asLong()
                val role = guild.getRoleById(roleId)

                if (role == null) {
                    return RabbitMQResponse.failure(
                        StatusCode.BadRequest,
                        InvalidErrorCodes.InvalidRole,
                        "The specified role was not found"
                    )
                }

                when (parts[1]) {
                    // user:role:add
                    "add" -> {
                        guild.addRoleToMember(member, role).queue({
                            RabbitMQResponse.success()
                                .sendAndAck(rabbitMqChannel, properties, envelope)
                        }, {
                            ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                        })
                        return null
                    }
                    // user:role:remove
                    "remove" -> {
                        guild.removeRoleFromMember(member, role).queue({
                            RabbitMQResponse.success()
                                .sendAndAck(rabbitMqChannel, properties, envelope)
                        }, {
                            ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                        })
                        return null
                    }
                    // user:role:has
                    "has" -> {
                        val hasRole = member.roles.contains(role)
                        val node = Main.json.createObjectNode()
                        node.put("hasRole", hasRole)
                        return RabbitMQResponse.success(node)
                    }

                }
            }

            "voice" -> {
                when (parts[1]) {
                    "deafen" -> {
                        val state = body.get("deafen").asBoolean()
                        member.deafen(state).queue({
                            RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
                        },
                            {
                                ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
                            })
                        return null
                    }

                    "mute" -> {
                        val state = body.get("mute").asBoolean()
                        member.deafen(state).queue({
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