package org.cascadebot.bot

import dev.minn.jda.ktx.messages.EmbedBuilder
import dev.minn.jda.ktx.messages.InlineEmbed
import org.cascadebot.bot.utils.Colors
import java.awt.Color
import net.dv8tion.jda.api.interactions.commands.OptionType as JdaOptionType

enum class SlotType {
    CUSTOM_CMD,
    PROVIDED,
    TEXT,
    AUTO_REPLY
}

enum class CustomCommandType {
    SLASH,
    CONTEXT_USER,
    CONTEXT_MESSAGE
}

enum class ScriptLang {
    JS,
    TEXT
}

enum class OptionType(val jdaOptionType: JdaOptionType) {
    SUB_COMMAND(JdaOptionType.SUB_COMMAND),
    SUBCOMMAND_GROUP(JdaOptionType.SUB_COMMAND_GROUP),
    USER(JdaOptionType.USER),
    MEMBER(JdaOptionType.USER),
    ATTACHMENT(JdaOptionType.ATTACHMENT),
    ROLE(JdaOptionType.ROLE),
    STRING(JdaOptionType.STRING),
    NUMBER(JdaOptionType.NUMBER),
    CHANNEL(JdaOptionType.CHANNEL),
    BOOLEAN(JdaOptionType.BOOLEAN)
}

enum class MessageType {
    INFO,
    SUCCESS,
    DANGER,
    WARNING,
    NEUTRAL;

    val color: Color?
        get() = when (this) {
            INFO -> Colors.INFO
            SUCCESS -> Colors.SUCCESS
            DANGER -> Colors.DANGER
            WARNING -> Colors.WARNING
            NEUTRAL -> null
        }

    val embed: InlineEmbed
        get() {
            val col = this.color
            return EmbedBuilder {
                color = col?.rgb
            }
        }

    companion object {
        fun fromColor(color: Color): MessageType? {
            return values().firstOrNull { it.color == color }
        }
    }
}