package org.cascadebot.bot.utils

import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji

val Emoji.idOrName
    get() = if (this.type == Emoji.Type.UNICODE) {
        (this as UnicodeEmoji).name
    } else {
        (this as CustomEmoji).id
    }