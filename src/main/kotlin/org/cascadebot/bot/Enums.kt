package org.cascadebot.bot

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