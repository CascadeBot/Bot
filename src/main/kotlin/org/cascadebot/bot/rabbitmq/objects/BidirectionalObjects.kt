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

data class PermissionOverrideData(
    val holderId: Long,
    val holderType: HolderType,
    val permissions: List<PermissionOverridePermission>
) : ISnowflake {

    companion object {

        fun fromPermissionOverride(override: PermissionOverride): PermissionOverrideData {
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
            return PermissionOverrideData(override.permissionHolder!!.idLong, type, perms)
        }
    }

    override fun getIdLong(): Long {
        return holderId
    }
}

data class EmbedFieldData(val name: String, val value: String, val inline: Boolean)

data class EmbedFooterData(val text: String, val iconUrl: String?)

data class EmbedAuthorData(val name: String, val url: String?, val iconUrl: String?)

data class EmbedData(
    val title: String?,
    val description: String?,
    val url: String?,
    val timestamp: Instant?,
    val footer: EmbedFooterData?,
    val image: String?,
    val thumbnail: String?,
    val author: EmbedAuthorData?,
    val fields: List<EmbedFieldData>?,
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

        fun fromDiscordEmbed(discEmbed: MessageEmbed): EmbedData {

            val fields = discEmbed.fields.map {
                EmbedFieldData(it.name!!, it.value!!, it.isInline)
            }

            val author = discEmbed.author?.let {
                EmbedAuthorData(it.name!!, it.url, it.iconUrl)
            }

            val footer = discEmbed.footer?.let {
                EmbedFooterData(it.text!!, it.iconUrl)
            }

            val messageType = discEmbed.color?.let {
                MessageType.fromColor(it)
            }

            return EmbedData(
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