package org.cascadebot.bot.rabbitmq.objects

import org.cascadebot.bot.MessageType
import java.awt.Color
import java.time.Instant
import java.util.Date

data class UserIDObject(val userId: String)

data class EmbedField(val title: String, val content: String, val inline: Boolean)

data class EmbedFooter(val text: String, val iconUrl: String?)

data class EmbedAuthor(val name: String, val url: String?, val iconUrl: String?)

sealed class Embed(
    val title: String?,
    val description: String?,
    val url: String?,
    val timestamp: Instant?,
    val footer: EmbedFooter?,
    val image: String?,
    val thumbnail: String?,
    val author: EmbedAuthor?,
    val fields: List<EmbedField>?
) {
    data class EmbedTyped(val type: MessageType)

    data class EmbedCustom(val color: Color?)
}