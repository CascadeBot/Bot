package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.RoleResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import org.cascadebot.bot.utils.PaginationUtil

class UserProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        context: RabbitMQContext,
        guild: Guild
    ): RabbitMQResponse<*>? {
        if (parts.size <= 1) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        val userId = body.get("user_id").asLong()

        val member = guild.getMemberById(userId)

        if (member == null) {
            return RabbitMQResponse.failure(
                StatusCode.BadRequest,
                InvalidErrorCodes.InvalidUser,
                "The specified member was not found"
            )
        }

        return when {
            checkAction(parts, "list", "role") -> listUserRoles(body, member)
            checkAction(parts, "color", "get") -> RabbitMQResponse.success("color", member.color)

            checkAction(parts, "permission") -> {
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

                return when {
                    checkAction(parts, "permission", "has") -> RabbitMQResponse.success(
                        "has_perm",
                        member.hasPermission(permission)
                    )

                    else -> CommonResponses.UNSUPPORTED_ACTION
                }
            }

            checkAction(parts, "nick", "set") -> setUserNickname(body, member, context)

            checkAction(parts, "role") -> {
                val roleId = body.get("role").asLong()
                val role = guild.getRoleById(roleId)

                if (role == null) {
                    return RabbitMQResponse.failure(
                        StatusCode.BadRequest,
                        InvalidErrorCodes.InvalidRole,
                        "The specified role was not found"
                    )
                }

                return when {
                    // user:role:add
                    checkAction(parts, "role", "add") -> {
                        return addUserRole(guild, member, role, context)
                    }
                    // user:role:remove
                    checkAction(parts, "role", "remove") -> {
                        return removeUserRole(guild, member, role, context)
                    }
                    // user:role:has
                    checkAction(parts, "role", "has") -> {
                        return RabbitMQResponse.success("has_role", member.roles.contains(role))
                    }

                    else -> CommonResponses.UNSUPPORTED_ACTION
                }
            }

            checkAction(parts, "voice", "deafen") -> deafenUser(body, member, context)
            checkAction(parts, "voice", "mute") -> muteUser(body, member, context)

            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun muteUser(
        body: ObjectNode,
        member: Member,
        context: RabbitMQContext
    ): Nothing? {
        val state = body.get("mute").asBoolean()
        member.deafen(state).queue(
            {
                RabbitMQResponse.success().sendAndAck(context)
            },
            ErrorHandler.handleError(context)
        )
        return null
    }

    private fun deafenUser(
        body: ObjectNode,
        member: Member,
        context: RabbitMQContext
    ): Nothing? {
        val state = body.get("deafen").asBoolean()
        member.deafen(state).queue(
            {
                RabbitMQResponse.success().sendAndAck(context)
            },
            ErrorHandler.handleError(context)
        )
        return null
    }

    private fun removeUserRole(
        guild: Guild,
        member: Member,
        role: Role,
        context: RabbitMQContext
    ): Nothing? {
        guild.removeRoleFromMember(member, role).queue({
            RabbitMQResponse.success()
                .sendAndAck(context)
        }, ErrorHandler.handleError(context))
        return null
    }

    private fun addUserRole(
        guild: Guild,
        member: Member,
        role: Role,
        context: RabbitMQContext
    ): Nothing? {
        guild.addRoleToMember(member, role).queue(
            {
                RabbitMQResponse.success()
                    .sendAndAck(context)
            },
            ErrorHandler.handleError(context)
        )
        return null
    }

    private fun setUserNickname(
        body: ObjectNode,
        member: Member,
        context: RabbitMQContext
    ): Nothing? {
        val nick = body.get("nick").asText()
        member.modifyNickname(nick).queue({
            RabbitMQResponse.success()
                .sendAndAck(context)
        }, ErrorHandler.handleError(context))
        return null
    }


    private fun listUserRoles(
        body: ObjectNode,
        member: Member
    ): RabbitMQResponse<PaginationUtil.PaginationResult<RoleResponse>> {
        // TODO pagination
        val params = PaginationUtil.parsePaginationParameters(body)
        val response = params.paginate(member.roles.map { RoleResponse.fromRole(it) })
        return RabbitMQResponse.success(response)
    }
}