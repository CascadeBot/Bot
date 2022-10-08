package org.cascadebot.bot.components

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionComponent
import net.dv8tion.jda.api.interactions.components.Component
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

class CascadeLinkButton private constructor(
    val link: String,
    val label: String?,
    val emoji: Emoji?,
    persistent: Boolean
) : CascadeComponent(link, persistent) {

    override val discordComponent: ActionComponent = Button.of(ButtonStyle.LINK, link, label, emoji)
    override val componentType: Component.Type = Component.Type.BUTTON

    init {
        require(label != null || emoji != null) { "Label and emoji cannot both be null" }
    }

    companion object {

        fun of(link: String, label: String? = null, emoji: Emoji? = null) = CascadeLinkButton(link, label, emoji, false)
    }

}