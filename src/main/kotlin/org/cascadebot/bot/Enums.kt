package org.cascadebot.bot

import org.cascadebot.bot.utils.Colors
import java.awt.Color

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

enum class OptionType {
    SUB_COMMAND,
    SUBCOMMAND_GROUP,
    USER,
    ATTACHMENT,
    ROLE,
    STRING,
    NUMBER,
    CHANNEL,
    BOOLEAN
}

enum class MessageType() {
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
}