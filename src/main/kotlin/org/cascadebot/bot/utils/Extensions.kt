package org.cascadebot.bot.utils

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji

/**
 * Returns a representation of an emoji depending on the type:
 * - For [Emoji.Type.UNICODE], the Unicode character from [UnicodeEmoji.getName] is used.
 * - For [Emoji.Type.CUSTOM], the emote's ID from [CustomEmoji.getId] is used.
 *
 * @author CascadeBot
 */
val Emoji.idOrName
    get() = if (this.type == Emoji.Type.UNICODE) {
        (this as UnicodeEmoji).name
    } else {
        (this as CustomEmoji).id
    }

/**
 * Helper function to create and configure a [SimpleModule] and then add it to the [JsonMapper.Builder].
 *
 * @author CascadeBot
 */
fun JsonMapper.Builder.addModule(builder: SimpleModule.()->Unit) {
    val mod = SimpleModule()
    builder(mod)
    addModule(mod)
}