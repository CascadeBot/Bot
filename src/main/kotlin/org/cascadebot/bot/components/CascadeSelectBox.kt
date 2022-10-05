package org.cascadebot.cascadebot.utils.interactions

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionComponent
import net.dv8tion.jda.api.interactions.components.Component
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import org.cascadebot.bot.components.CascadeComponent
import org.cascadebot.bot.components.InteractionMessage
import org.cascadebot.bot.utils.idOrName

typealias SelectionRunnable = (runner: Member, owner: Member, channel: TextChannel, message: InteractionMessage, selected: List<String>) -> Unit

class CascadeSelectBox(uniqueId: String, persistent: Boolean, val consumer: SelectionRunnable) :
    CascadeComponent(uniqueId, persistent) {

    private val builder: SelectMenu.Builder = SelectMenu.create(uniqueId)

    var minSelect = 1
        set(value) {
            if (value < 1 || value > maxSelect) {
                throw UnsupportedOperationException("Minimum selection number cannot be less then 1 or greater then the max select value")
            }
            field = value
        }
    var maxSelect = 1
        set(value) {
            if (value > 25 || value < minSelect) {
                throw UnsupportedOperationException("Maximum selection number cannot be greater then 25 or less then the min select value")
            }
            field = value
        }

    private val defaults: MutableList<String> = mutableListOf()

    override val discordComponent: ActionComponent
        get() {
            builder.maxValues = maxSelect;
            builder.minValues = minSelect;
            builder.setDefaultValues(defaults)
            return builder.build()
        }

    override val componentType: Component.Type = Component.Type.SELECT_MENU

    fun addOption(label: String, default: Boolean = false): CascadeSelectBox {
        builder.addOption(label, label)
        handleDefault(label, default)
        return this
    }

    fun addOption(label: String, description: String, default: Boolean = false): CascadeSelectBox {
        builder.addOption(label, label, description)
        handleDefault(label, default)
        return this
    }

    fun addOption(label: String, emoji: Emoji, default: Boolean = false): CascadeSelectBox {
        val value = "$label-" + emoji.idOrName
        builder.addOption(label, value, emoji)
        handleDefault(value, default)
        return this
    }

    fun addOption(label: String, description: String, emoji: Emoji, default: Boolean = false): CascadeSelectBox {
        val value = "$label-" + emoji.idOrName
        builder.addOption(label, value, description, emoji)
        handleDefault(value, default)
        return this
    }

    private fun handleDefault(value: String, default: Boolean) {
        if (default) {
            if (defaults.size >= maxSelect) {
                throw UnsupportedOperationException("Cannot add more defaults then the user can select")
            }
            defaults.add(value)
        }
    }

    fun setPlaceholder(placeHolder: String): CascadeSelectBox {
        builder.placeholder = placeHolder
        return this
    }

    fun persistent(persistent: Boolean = true): CascadeSelectBox {
        this.persistent = persistent
        return this
    }

    fun addDefault(value: String): CascadeSelectBox {
        handleDefault(value, true)
        return this
    }

    fun clearDefaults() {
        defaults.clear()
    }

}