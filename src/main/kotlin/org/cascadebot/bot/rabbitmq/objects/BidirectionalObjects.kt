package org.cascadebot.bot.rabbitmq.objects

import com.fasterxml.jackson.annotation.JsonProperty
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ISnowflake
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.PermissionOverride
import org.cascadebot.bot.MessageType
import java.awt.Color
import java.time.Instant

enum class HolderType {
    ROLE,
    USER
}

enum class PermissionOverrideState {
    ALLOW,
    DENY,
    NEUTRAL
}

data class PermissionOverridePermission(val permission: Permission, val state: PermissionOverrideState)

data class RMQPermissionOverride(
    val holderId: Long,
    val holderType: HolderType,
    val permissions: List<PermissionOverridePermission>
) : ISnowflake {
    companion object {
        fun fromPermissionOverride(override: PermissionOverride): RMQPermissionOverride {
            val perms: MutableList<PermissionOverridePermission> = mutableListOf()
            for (discordPerm in Permission.values()) {
                var state = PermissionOverrideState.NEUTRAL
                if (override.allowed.contains(discordPerm)) {
                    state = PermissionOverrideState.ALLOW
                }
                if (override.denied.contains(discordPerm)) {
                    state = PermissionOverrideState.DENY
                }
                perms.add(PermissionOverridePermission(discordPerm, state))
            }
            val type = if (override.isMemberOverride) {
                HolderType.USER
            } else {
                HolderType.ROLE
            }
            return RMQPermissionOverride(override.permissionHolder!!.idLong, type, perms)
        }
    }

    override fun getIdLong(): Long {
        return holderId
    }
}

data class RMQEmbedField(val name: String, val value: String, val inline: Boolean)

data class RMQEmbedFooter(val text: String, val iconUrl: String?)

data class RMQEmbedAuthor(val name: String, val url: String?, val iconUrl: String?)

data class RMQEmbed(
    val title: String?,
    val description: String?,
    val url: String?,
    val timestamp: Instant?,
    val footer: RMQEmbedFooter?,
    val image: String?,
    val thumbnail: String?,
    val author: RMQEmbedAuthor?,
    val fields: List<RMQEmbedField>?,
    val messageType: MessageType?,
    @JsonProperty("color") private val col: Color?
) {

    val color
        get() = if (messageType != null) messageType.color else col

    fun toDiscordEmbed(): MessageEmbed {
        val builder = EmbedBuilder()
        builder.setTitle(title, url)
        builder.setDescription(description)
        builder.setColor(color)
        builder.setTimestamp(timestamp)
        builder.setImage(image)
        builder.setThumbnail(thumbnail)

        if (footer != null) {
            builder.setFooter(footer.text, footer.iconUrl)
        }

        if (author != null) {
            builder.setAuthor(author.name, author.url, author.iconUrl)
        }

        fields?.forEach {
            builder.addField(it.name, it.value, it.inline)
        }

        return builder.build()
    }

    companion object {
        fun fromDiscordEmbed(discEmbed: MessageEmbed): RMQEmbed {

            val fields = discEmbed.fields.map {
                RMQEmbedField(it.name!!, it.value!!, it.isInline)
            }

            val author = discEmbed.author?.let {
                RMQEmbedAuthor(it.name!!, it.url, it.iconUrl)
            }

            val footer = discEmbed.footer?.let {
                RMQEmbedFooter(it.text!!, it.iconUrl)
            }

            val messageType = discEmbed.color?.let {
                MessageType.fromColor(it)
            }

            return RMQEmbed(
                discEmbed.title,
                discEmbed.description,
                discEmbed.url,
                discEmbed.timestamp?.toInstant(),
                footer,
                discEmbed.image?.url,
                discEmbed.thumbnail?.url,
                author,
                fields,
                messageType,
                discEmbed.color
            )
        }
    }

}