package org.cascadebot.bot.components

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import net.dv8tion.jda.api.requests.restaction.MessageEditAction
import net.dv8tion.jda.api.utils.messages.MessageEditData

class InteractionMessage(val message: Message, val container: ComponentContainer) {

    /**
     * Message ID
     */
    val idLong: Long = message.idLong

    fun editMessage(content: String): MessageEditAction {
        return message.editMessage(content)
            .setReplace(true)
            .setComponents(container.getComponents().map { it.toDiscordActionRow() })
    }

    fun editMessage(embed: MessageEmbed): MessageEditAction {
        return message.editMessageEmbeds(embed)
            .setReplace(true)
            .setComponents(container.getComponents().map { it.toDiscordActionRow() })
    }

    fun editMessage(message: Message): MessageEditAction {
        return message.editMessage(MessageEditData.fromMessage(message))
            .setReplace(true)
            .setComponents(container.getComponents().map { it.toDiscordActionRow() })
    }

    fun notifyContainerChange(): MessageEditAction {
        return message.editMessageComponents().setComponents(container.getComponents().map { it.toDiscordActionRow() })
    }

    fun deleteMessage(): AuditableRestAction<Void> {
        return message.delete()
    }

}