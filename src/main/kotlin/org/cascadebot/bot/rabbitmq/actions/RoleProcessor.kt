package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.RoleMoved
import org.cascadebot.bot.rabbitmq.objects.RolePermission
import org.cascadebot.bot.rabbitmq.objects.RoleTagsResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler

class RoleProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        guild: Guild
    ): RabbitMQResponse<*>? {
        if (parts.size <= 1) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        val roleId = body.get("role_id").asLong()

        val role = guild.getRoleById(roleId)

        if (role == null) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidRole,
                "The specified role was not found"
            )
        }

        return when {
            checkAction(parts, "permissions", "list") -> listRolePermissions(role)
            checkAction(parts, "permissions", "set") -> setRolePermissions(
                body,
                role,
                rabbitMqChannel,
                properties,
                envelope
            )

            checkAction(parts, "position", "set") -> setRolePosition(
                body,
                role,
                guild,
                rabbitMqChannel,
                properties,
                envelope
            )

            checkAction(parts, "tags", "get") -> RabbitMQResponse.success(RoleTagsResponse.fromTags(role.tags))

            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun setRolePosition(
        body: ObjectNode,
        role: Role,
        guild: Guild,
        rabbitMqChannel: Channel,
        properties: AMQP.BasicProperties,
        envelope: Envelope
    ): Nothing? {
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
                } else ErrorHandler.handleError(envelope, properties, rabbitMqChannel, it)
            }
        )
        return null
    }

    private fun setRolePermissions(
        body: ObjectNode,
        role: Role,
        rabbitMqChannel: Channel,
        properties: AMQP.BasicProperties,
        envelope: Envelope
    ): Nothing? {
        val permNode = body.get("perm")
        val perm = Permission.valueOf(permNode.get("permission").asText().uppercase())
        val state = permNode.get("state").asBoolean()
        if (state) {
            role.manager.givePermissions(perm).queue({
                RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
            }, ErrorHandler.handleError(envelope, properties, rabbitMqChannel))
        } else {
            role.manager.revokePermissions(perm).queue({
                RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
            }, ErrorHandler.handleError(envelope, properties, rabbitMqChannel))
        }
        return null
    }

    private fun listRolePermissions(role: Role): RabbitMQResponse<List<RolePermission>> {
        val perms = mutableListOf<RolePermission>()
        for (perm in Permission.entries) {
            perms.add(RolePermission(perm, role.hasPermission(perm)))
        }
        return RabbitMQResponse.success(perms)
    }
}