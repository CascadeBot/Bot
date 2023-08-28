package org.cascadebot.bot.rabbitmq.actions.channel

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.actions.Processor
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.HolderType
import org.cascadebot.bot.rabbitmq.objects.PermissionOverrideData
import org.cascadebot.bot.rabbitmq.objects.PermissionOverridePermission
import org.cascadebot.bot.rabbitmq.objects.PermissionOverrideState
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import org.cascadebot.bot.utils.PaginationUtil
import org.cascadebot.bot.utils.createJsonObject

class GenericChannelProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        envelope: Envelope,
        properties: AMQP.BasicProperties,
        rabbitMqChannel: Channel,
        guild: Guild
    ): RabbitMQResponse<*>? {
        val channel = ChannelUtils.validateAndGetChannel(body, guild)

        if (channel == null) {
            return CommonResponses.CHANNEL_NOT_FOUND
        }

        return when {
            checkAction(parts, "name", "set") -> setChannelName(channel, body, rabbitMqChannel, properties, envelope)
            checkAction(parts, "permissions", "list") -> listChannelPermissions(body, channel)
            checkAction(parts, "permissions", "create") -> createChannelPermission(
                body,
                guild,
                channel,
                rabbitMqChannel,
                properties,
                envelope
            )
            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun createChannelPermission(
        body: ObjectNode,
        guild: Guild,
        channel: GuildChannel,
        rabbitMqChannel: Channel,
        properties: AMQP.BasicProperties,
        envelope: Envelope
    ): Nothing? {
        val override =
            Main.json.treeToValue(body.get("override"), PermissionOverrideData::class.java)
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
        channel.permissionContainer.manager.putPermissionOverride(holder, cal.allow, cal.deny).queue(
            {
                RabbitMQResponse.success().sendAndAck(rabbitMqChannel, properties, envelope)
            },
            ErrorHandler.handleError(envelope, properties, rabbitMqChannel)
        )
        return null
    }

    private fun listChannelPermissions(
        body: ObjectNode,
        channel: GuildChannel
    ): RabbitMQResponse<PaginationUtil.PaginationResult<PermissionOverrideData>> {
        val params = PaginationUtil.parsePaginationParameters(body)
        return RabbitMQResponse.success(
            params.paginate(
                channel.permissionContainer.permissionOverrides.map {
                    PermissionOverrideData.fromPermissionOverride(
                        it
                    )
                }
            )
        )
    }

    private fun setChannelName(
        channel: GuildChannel,
        body: ObjectNode,
        rabbitMqChannel: Channel,
        properties: AMQP.BasicProperties,
        envelope: Envelope
    ): Nothing? {
        val oldName = channel.name
        val newName = body.get("name").asText()
        channel.manager.setName(newName).queue({
            val node = createJsonObject(
                "old_name" to oldName,
                "new_name" to newName
            )
            RabbitMQResponse.success(node).sendAndAck(rabbitMqChannel, properties, envelope)
        }, ErrorHandler.handleError(envelope, properties, rabbitMqChannel))
        return null
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