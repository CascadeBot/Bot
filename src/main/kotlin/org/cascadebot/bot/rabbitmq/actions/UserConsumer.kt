package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.exceptions.HierarchyException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import okhttp3.internal.toHexString
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MiscErrorCodes
import org.cascadebot.bot.rabbitmq.objects.PermissionsErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import java.util.Locale
import java.util.function.Consumer
import kotlin.IllegalArgumentException

class UserConsumer : ActionConsumer {

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

        val userId = body.get("user").get("id").asLong()
        val guildId = body.get("user").get("guild").asLong()

        val guild = Main.shardManager.getShardById(shard)?.getGuildById(guildId)
        val member = guild?.getMemberById(userId)

        if (member == null) {
            RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidUser,
                "The specified member was not found"
            ).sendAndAck(channel, properties, envelope)
            return
        }

        when (parts[0]) {
            "list" -> {
                // TODO pagination
                when(parts[1]) {
                    // user:list:role
                    "role" -> {

                    }
                }
            }
            "color" -> {
                // user:color:get
                if (parts[1] == "get") {
                    val node = Main.json.createObjectNode()
                    node.put("color", member.colorRaw.toHexString())
                    RabbitMQResponse.success(node)
                        .sendAndAck(channel, properties, envelope)
                    return
                }
            }
            "permission" -> {
                val permission: Permission
                try {
                    permission = Permission.valueOf(body.get("permission").asText().uppercase())
                } catch (e: IllegalArgumentException) {
                    RabbitMQResponse.failure(
                        StatusCode.BadRequest,
                        InvalidErrorCodes.InvalidPermission,
                        "The specified permission doesn't exist"
                    ).sendAndAck(channel, properties, envelope)
                    return
                }
                if (parts[1] == "has") {
                    val node = Main.json.createObjectNode()
                    node.put("hasPerm", member.hasPermission(permission))
                    RabbitMQResponse.success(node)
                        .sendAndAck(channel, properties, envelope)
                    return
                }
            }
            "nick" -> {
                val nick = body.get("nick").asText()
                // user:nick:set
                if (parts[1] == "set") {
                    member.modifyNickname(nick).queue({
                        RabbitMQResponse.success()
                            .sendAndAck(channel, properties, envelope)
                    }, {
                        handleError(envelope, properties, channel, it)
                    })
                    return
                }
            }
            "role" -> {
                val roleId = body.get("role").asLong()
                val role = guild.getRoleById(roleId)

                if (role == null) {
                    RabbitMQResponse.failure(
                        StatusCode.BadRequest,
                        InvalidErrorCodes.InvalidRole,
                        "The specified role was not found"
                    ).sendAndAck(channel, properties, envelope)
                    return
                }

                when (parts[1]) {
                    // user:role:add
                    "add" -> {
                        guild.addRoleToMember(member, role).queue({
                            RabbitMQResponse.success()
                                .sendAndAck(channel, properties, envelope)
                        }, {
                            handleError(envelope, properties, channel, it)
                        })
                        return
                    }
                    // user:role:remove
                    "remove" -> {
                        guild.removeRoleFromMember(member, role).queue({
                            RabbitMQResponse.success()
                                .sendAndAck(channel, properties, envelope)
                        }, {
                            handleError(envelope, properties, channel, it)
                        })
                        return
                    }
                    // user:role:has
                    "has" -> {
                        val hasRole = member.roles.contains(role)
                        val node = Main.json.createObjectNode()
                        node.put("hasRole", hasRole)
                        RabbitMQResponse.success(node)
                            .sendAndAck(channel, properties, envelope)
                    }

                }
            }

        }

        RabbitMQResponse.failure(
            StatusCode.BadRequest,
            InvalidErrorCodes.InvalidAction,
            "The specified action is not supported"
        ).sendAndAck(channel, properties, envelope)
    }

    private fun handleError(
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        channel: Channel,
        throwable: Throwable
    ) {
        when (throwable) {
            is InsufficientPermissionException -> {
                RabbitMQResponse.failure(
                    StatusCode.DiscordException,
                    PermissionsErrorCodes.MissingPermission,
                    "The bot is missing the permission " + throwable.permission.name + " required to do this"
                ).sendAndAck(channel, properties, envelope)
            }

            is HierarchyException -> {
                RabbitMQResponse.failure(
                    StatusCode.DiscordException,
                    PermissionsErrorCodes.CannotInteract,
                    "Cannot modify this user as they are higher then the bot!"
                ).sendAndAck(channel, properties, envelope)
            }

            else -> {
                RabbitMQResponse.failure(
                    StatusCode.ServerException,
                    MiscErrorCodes.UnexpectedError,
                    "Received an unexpected error " + throwable.javaClass.name + ": " + throwable.message
                ).sendAndAck(channel, properties, envelope)
            }
        }
    }
}