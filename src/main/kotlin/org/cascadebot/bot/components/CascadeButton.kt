package org.cascadebot.bot.components

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionComponent
import net.dv8tion.jda.api.interactions.components.Component
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.cascadebot.bot.utils.idOrName

typealias ButtonRunnable = (runner: Member, owner: Member, channel: TextChannel, message: InteractionMessage) -> Unit

class CascadeButton private constructor(
    val type: ButtonStyle,
    val label: String?,
    val emoji: Emoji?,
    persistent: Boolean = false,
    val consumer: ButtonRunnable
) : CascadeComponent(
    generateId(label, emoji), persistent
) {

    var disabled: Boolean = false

    override val discordComponent: ActionComponent = Button.of(type, id, label, emoji).withDisabled(disabled)
    override val componentType: Component.Type = Component.Type.BUTTON

    init {
        require(label != null && emoji != null) { "Label and emoji cannot both be null" }
        if (type == ButtonStyle.LINK) {
            throw UnsupportedOperationException("Please use CascadeLinkButton if trying to use a link button") // TODO implement link buttons
        }
    }

    companion object {

        fun primary(label: String, consumer: ButtonRunnable) =
            CascadeButton(ButtonStyle.PRIMARY, label, null, false, consumer)

        fun primary(emoji: Emoji, consumer: ButtonRunnable) =
            CascadeButton(ButtonStyle.PRIMARY, null, emoji, false, consumer)

        fun primary(label: String, emoji: Emoji, consumer: ButtonRunnable) =
            CascadeButton(ButtonStyle.PRIMARY, label, emoji, false, consumer)

        fun secondary(label: String, consumer: ButtonRunnable) =
            CascadeButton(ButtonStyle.SECONDARY, label, null, false, consumer)

        fun secondary(emoji: Emoji, consumer: ButtonRunnable) =
            CascadeButton(ButtonStyle.SECONDARY, null, emoji, false, consumer)

        fun secondary(label: String, emoji: Emoji, consumer: ButtonRunnable) =
            CascadeButton(ButtonStyle.SECONDARY, label, emoji, false, consumer)

        fun success(label: String, consumer: ButtonRunnable) =
            CascadeButton(ButtonStyle.SUCCESS, label, null, false, consumer)

        fun success(emoji: Emoji, consumer: ButtonRunnable) =
            CascadeButton(ButtonStyle.SUCCESS, null, emoji, false, consumer)

        fun success(label: String, emoji: Emoji, consumer: ButtonRunnable) =
            CascadeButton(ButtonStyle.SUCCESS, label, emoji, false, consumer)

        fun danger(label: String, consumer: ButtonRunnable) =
            CascadeButton(ButtonStyle.DANGER, label, null, false, consumer)

        fun danger(emoji: Emoji, consumer: ButtonRunnable) =
            CascadeButton(ButtonStyle.DANGER, null, emoji, false, consumer)

        fun danger(label: String, emoji: Emoji, consumer: ButtonRunnable) =
            CascadeButton(ButtonStyle.DANGER, label, emoji, false, consumer)

        fun persistent(type: ButtonStyle, label: String, consumer: ButtonRunnable) =
            CascadeButton(type, label, null, true, consumer)

        fun persistent(type: ButtonStyle, emoji: Emoji, consumer: ButtonRunnable) =
            CascadeButton(type, null, emoji, true, consumer)

        fun persistent(type: ButtonStyle, label: String, emoji: Emoji, consumer: ButtonRunnable) =
            CascadeButton(type, label, emoji, true, consumer)

    }

}

private fun generateId(label: String?, emoji: Emoji?): String {
    require(!(label == null && emoji == null)) { "Both the label and emoji cannot be null!" }
    return if (label != null) {
        if (emoji != null) {
            "$label-${emoji.idOrName}"
        } else {
            label
        }
    } else emoji?.toString() ?: throw IllegalStateException("Both label and emoji cannot be null!")
}