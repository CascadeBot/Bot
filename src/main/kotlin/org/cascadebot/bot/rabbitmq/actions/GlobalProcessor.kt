package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel
import org.cascadebot.bot.rabbitmq.objects.ChannelResponse
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.InvalidErrorCodes
import org.cascadebot.bot.rabbitmq.objects.MemberResponse
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.RoleResponse
import org.cascadebot.bot.rabbitmq.objects.StatusCode
import org.cascadebot.bot.utils.PaginationUtil

class GlobalProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        guild: Guild
    ): RabbitMQResponse<*> {
        if (parts.size <= 1) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        return when {
            checkAction(parts, "user", "byId") -> getUserById(body, guild)
            checkAction(parts, "user", "byName") -> getUserByName(body, guild)
            checkAction(parts, "role", "byId") -> getRoleById(body, guild)
            checkAction(parts, "role", "byName") -> getRoleByName(body, guild)
            checkAction(parts, "channel", "byId") -> getChannelById(body, guild)
            checkAction(parts, "channel", "byName") -> getChannelByName(body, guild)
            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun getChannelByName(
        body: ObjectNode,
        guild: Guild
    ): RabbitMQResponse<out PaginationUtil.PaginationResult<ChannelResponse>> {
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

    private fun getChannelById(
        body: ObjectNode,
        guild: Guild
    ): RabbitMQResponse<out ChannelResponse> {
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

    private fun getRoleByName(
        body: ObjectNode,
        guild: Guild
    ): RabbitMQResponse<out PaginationUtil.PaginationResult<RoleResponse>> {
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

    private fun getRoleById(
        body: ObjectNode,
        guild: Guild
    ): RabbitMQResponse<out RoleResponse> {
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

    private fun getUserByName(
        body: ObjectNode,
        guild: Guild
    ): RabbitMQResponse<out PaginationUtil.PaginationResult<MemberResponse>> {
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

    private fun getUserById(
        body: ObjectNode,
        guild: Guild
    ): RabbitMQResponse<out MemberResponse> {
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
}