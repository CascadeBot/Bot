package org.cascadebot.bot.rabbitmq.objects

import com.fasterxml.jackson.databind.JsonNode
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.ISnowflake
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.Role.RoleTags
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel
import org.cascadebot.bot.CustomCommandType
import org.cascadebot.bot.ScriptLang
import org.cascadebot.bot.db.entities.AutoResponderEntity
import org.cascadebot.bot.db.entities.CustomCommandEntity
import org.cascadebot.bot.db.entities.GuildSlotEntity
import java.awt.Color
import java.util.UUID

data class UserResponse(val id: String, val name: String, val avatarUrl: String, val discriminator: String) :
    IRMQResponse {

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
) : ISnowflake, IRMQResponse {

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
) : ISnowflake, IRMQResponse {

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
        return id
    }
}

data class RoleTagsResponse(
    val botId: String?,
    val integrationId: String?,
    val bot: Boolean,
    val boost: Boolean,
    val integration: Boolean
) : IRMQResponse {

    companion object {

        fun fromTags(tags: RoleTags) = RoleTagsResponse(
            tags.botId,
            tags.integrationId,
            tags.isBot,
            tags.isBoost,
            tags.isIntegration,
        )
    }

}

data class RolePermission(val permission: Permission, val state: Boolean) : IRMQResponse

data class RoleMoved(val prevPos: Int, val newPos: Int) : IRMQResponse

data class ChannelResponse(val id: Long, val name: String, val type: String, val position: Int) : ISnowflake,
    IRMQResponse {

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
) : IRMQResponse {

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

interface SlotEntry : IRMQResponse {

    val slotId: UUID
    val enabled: Boolean?
}

data class CustomCommandResponse(
    override val slotId: UUID,
    override val enabled: Boolean?,
    val name: String,
    val description: String?,
    val marketplaceReference: UUID?,
    val type: CustomCommandType,
    val scriptLang: ScriptLang,
    val entrypoint: UUID?,
    val ephemeral: Boolean?
) : IRMQResponse, SlotEntry {

    companion object {

        fun fromEntity(slot: GuildSlotEntity, entity: CustomCommandEntity): CustomCommandResponse {
            return CustomCommandResponse(
                entity.slotId,
                slot.enabled,
                entity.name,
                entity.description,
                entity.marketplaceRef,
                entity.type,
                entity.lang,
                entity.entrypoint,
                entity.ephemeral
            )
        }
    }

}

data class AutoResponderResponse(
    override val slotId: UUID,
    override val enabled: Boolean?,
    val text: JsonNode,
    val matchText: List<String>?
) : IRMQResponse, SlotEntry {

    companion object {

        fun fromEntity(slot: GuildSlotEntity, entity: AutoResponderEntity): AutoResponderResponse {
            return AutoResponderResponse(
                entity.slotId,
                slot.enabled,
                entity.text,
                entity.match
            )
        }

    }

}

