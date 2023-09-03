package org.cascadebot.bot.components

import net.dv8tion.jda.api.interactions.components.ActionComponent
import net.dv8tion.jda.api.interactions.components.Component
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.cascadebot.bot.Main
import org.cascadebot.bot.utils.ChannelId
import java.util.UUID

abstract class CascadeComponent(val uniqueId: String, persistent: Boolean) {

    val id = "${if (persistent) "persistent" else "cached"}-${UUID.randomUUID()}"
    var persistent: Boolean = persistent
        protected set

    abstract val discordComponent: ActionComponent
    abstract val componentType: Component.Type

    companion object {

        fun fromDiscordComponent(channelId: ChannelId, component: ActionComponent): CascadeComponent? {
            if (component.type == Component.Type.BUTTON && (component as Button).style == ButtonStyle.LINK) {
                return CascadeLinkButton.of(component.url!!, component.url, component.emoji)
            }

            val id = component.id ?: return null

            val prefix = id.substringBefore("-")

            return when (prefix) {
                "persistent" -> {
                    PersistentComponent.entries.find { it.component.discordComponent == component }?.component
                }

                "cached" -> {
                    Main.componentCache.componentCache.getIfPresent(channelId)?.getIfPresent(component.id)
                }

                else -> {
                    error("Invalid prefix ($prefix) on custom ID detected")
                }
            }
        }
    }

}