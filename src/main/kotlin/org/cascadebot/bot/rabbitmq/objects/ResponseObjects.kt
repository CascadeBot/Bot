package org.cascadebot.bot.rabbitmq.objects

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.ISnowflake
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import net.dv8tion.jda.api.utils.messages.MessageEditData
import org.cascadebot.bot.Main
import org.cascadebot.bot.rabbitmq.utils.ErrorHandler
import java.awt.Color

data class UserResponse(val id: String, val name: String, val avatarUrl: String, val discriminator: String) {

    companion object {
        fun fromUser(user: User): UserResponse {
            return UserResponse(user.id, user.name, user.effectiveAvatarUrl, user.discriminator)
        }
    }

}

data class MemberResponse(
    val id: Long,
    val name: String,
    val avatarUrl: String,
    val nickname: String?,
    val discriminator: String
) : ISnowflake {

    companion object {
        fun fromMember(member: Member): MemberResponse {
            return MemberResponse(
                member.idLong,
                member.user.name,
                member.effectiveAvatarUrl,
                member.nickname,
                member.user.discriminator
            )
        }
    }

    override fun getIdLong(): Long {
        return id
    }

}

data class RoleResponse(
    val id: Long,
    val name: String,
    val position: Int,
    val color: Color?,
    val icon: String?,
    val emoji: String?,
    val mentionable: Boolean = false,
    val hoisted: Boolean = false
) : ISnowflake {
    companion object {
        fun fromRole(role: Role): RoleResponse {
            return RoleResponse(
                role.idLong,
                role.name,
                role.position,
                role.color,
                role.icon?.iconUrl,
                role.icon?.emoji,
                role.isMentionable,
                role.isHoisted
            )
        }
    }

    override fun getIdLong(): Long {
        return id;
    }
}

data class RolePermission(val permission: Permission, val state: Boolean)

data class RoleMoved(val prevPos: Int, val newPos: Int)

data class ChannelResponse(val id: Long, val name: String, val type: String, val position: Int) : ISnowflake {
    companion object {
        fun fromChannel(channel: StandardGuildChannel): ChannelResponse {
            return ChannelResponse(
                channel.idLong,
                channel.name,
                channel.type.name.lowercase(),
                channel.position
            )
        }

        fun fromThread(channel: ThreadChannel): ChannelResponse {
            return ChannelResponse(
                channel.idLong,
                channel.name,
                channel.type.name.lowercase(),
                -1
            )
        }
    }

    override fun getIdLong(): Long {
        return id
    }
}

data class MutualGuildResponse(
    val id: Long,
    val name: String,
    val iconUrl: String?,
    val memberCount: Int,
    val onlineCount: Int
) {

    companion object {

        fun fromGuild(guild: Guild, guildMetadata: Guild.MetaData): MutualGuildResponse {
            return MutualGuildResponse(
                guild.idLong,
                guild.name,
                guild.iconUrl,
                guildMetadata.approximateMembers,
                guildMetadata.approximatePresences
            )
        }
    }
}

data class RabbitMqMessage(val messageId: Long, val channelId: Long, val content: String, val embeds: List<RMQEmbed>) : ISnowflake {

    companion object {
        fun fromDiscordMessage(message: Message): RabbitMqMessage {
            val embeds = message.embeds.map {
                RMQEmbed.fromDiscordEmbed(it)
            }
            return RabbitMqMessage(message.idLong, message.channel.idLong, message.contentRaw, embeds)
        }
    }

    override fun getIdLong(): Long {
       return messageId;
    }

    fun toDiscordCreateMessage(): MessageCreateData {
        val builder = MessageCreateBuilder()
        for (embedObj in embeds) {
            builder.addEmbeds(embedObj.toDiscordEmbed())
        }
        builder.setContent(content)
        return builder.build()
    }

    fun toDiscordEditMessage(): MessageEditData {
        val builder = MessageEditBuilder()
        builder.embeds.clear()
        for (embedObj in embeds) {
            builder.embeds.add(embedObj.toDiscordEmbed())
        }
        builder.setContent(content)
        return builder.build()
    }

}
