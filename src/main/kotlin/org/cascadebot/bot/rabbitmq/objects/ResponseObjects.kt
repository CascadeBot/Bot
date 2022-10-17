package org.cascadebot.bot.rabbitmq.objects

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel
import java.awt.Color

data class UserResponse(val id: String, val name: String, val avatarUrl: String, val discriminator: String) {

    companion object {
        fun fromUser(user: User): UserResponse {
            return UserResponse(user.id, user.name, user.effectiveAvatarUrl, user.discriminator)
        }
    }

}

data class MemberResponse(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val nickname: String?,
    val discriminator: String
) {

    companion object {
        fun fromMember(member: Member): MemberResponse {
            return MemberResponse(
                member.id,
                member.user.name,
                member.effectiveAvatarUrl,
                member.nickname,
                member.user.discriminator
            )
        }
    }

}

data class RoleResponse(
    val id: String,
    val name: String,
    val position: Int,
    val color: Color?,
    val icon: String?,
    val emoji: String?,
    val mentionable: Boolean = false,
    val hoisted: Boolean = false
) {
    companion object {
        fun fromRole(role: Role): RoleResponse {
            return RoleResponse(
                role.id,
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
}

data class RolePermission(val permission: Permission, val state: Boolean)

data class RoleMoved(val prevPos: Int, val newPos: Int)

data class ChannelResponse(val id: String, val name: String, val type: String, val position: Int) {
    companion object {
        fun fromChannel(channel: StandardGuildChannel): ChannelResponse {
            return ChannelResponse(
                channel.id,
                channel.name,
                channel.type.name.lowercase(),
                channel.position
            )
        }
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