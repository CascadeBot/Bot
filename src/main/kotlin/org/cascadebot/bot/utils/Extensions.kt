package org.cascadebot.bot.utils

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji

val Emoji.idOrName
    get() = if (this.type == Emoji.Type.UNICODE) {
        (this as UnicodeEmoji).name
    } else {
        (this as CustomEmoji).id
    }

fun JsonMapper.Builder.addModule(builder: SimpleModule.()->Unit) {
    val mod = SimpleModule()
    builder(mod)
    addModule(mod)
}