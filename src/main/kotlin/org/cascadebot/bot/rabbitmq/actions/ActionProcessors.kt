package org.cascadebot.bot.rabbitmq.actions

import org.cascadebot.bot.rabbitmq.actions.channel.ChannelWithThreadsProcessor
import org.cascadebot.bot.rabbitmq.actions.channel.GenericChannelProcessor
import org.cascadebot.bot.rabbitmq.actions.channel.MessageChannelProcessor
import org.cascadebot.bot.rabbitmq.actions.channel.MovableChannelProcessor
import org.cascadebot.bot.rabbitmq.actions.channel.TextChannelProcessor
import org.cascadebot.bot.rabbitmq.actions.channel.VoiceChanelProcessor

enum class ActionProcessors(val root: String, val processor: Processor, val requiresGuild: Boolean) {

    GLOBAL("global", GlobalProcessor(), true),
    USER("user", UserProcessor(), true),
    ROLE("role", RoleProcessor(), true),
    MESSAGE_CHANNEL("channel:message", MessageChannelProcessor(), true),
    GENERAL_CHANNEL("channel:general", GenericChannelProcessor(), true),
    MOVABLE_CHANNEL("channel:movable", MovableChannelProcessor(), true),
    THREADED_CHANNEL("channel:threaded", ChannelWithThreadsProcessor(), true),
    INTERACTION("channel:interaction", InteractionProcessor(), true),
    TEXT_CHANNEL("channel:text", TextChannelProcessor(), true),
    VOICE_CHANNEL("channel:voice", VoiceChanelProcessor(), true),
    MESSAGE("message", MessageProcessor(), true),
    SLOT("slot", SlotProcessor(), true),
    COMMAND("command", CommandProcessor(), true),
    AUTO_RESPONDER("auto_responder", AutoResponderProcessor(), true)

}