package org.cascadebot.bot.rabbitmq.actions

import org.cascadebot.bot.rabbitmq.actions.channel.ChannelWithThreadsProcessor
import org.cascadebot.bot.rabbitmq.actions.channel.GenericChannelProcessor
import org.cascadebot.bot.rabbitmq.actions.channel.MessageChannelProcessor
import org.cascadebot.bot.rabbitmq.actions.channel.MovableChannelProcessor
import org.cascadebot.bot.rabbitmq.actions.channel.TextChannelProcessor
import org.cascadebot.bot.rabbitmq.actions.channel.VoiceChanelProcessor

enum class ActionProcessors(val root: String, val processor: Processor) {

    GLOBAL("global", GlobalProcessor()),
    USER("user", UserProcessor()),
    ROLE("role", RoleProcessor()),
    MESSAGE_CHANNEL("channel:message", MessageChannelProcessor()),
    GENERAL_CHANNEL("channel:general", GenericChannelProcessor()),
    MOVABLE_CHANNEL("channel:movable", MovableChannelProcessor()),
    THREADED_CHANNEL("channel:threaded", ChannelWithThreadsProcessor()),
    INTERACTION("channel:interaction", InteractionProcessor()),
    TEXT_CHANNEL("channel:text", TextChannelProcessor()),
    VOICE_CHANNEL("channel:voice", VoiceChanelProcessor()),
    MESSAGE("message", MessageProcessor()),
    SLOT("slot", SlotProcessor()),
    COMMAND("command", CommandProcessor()),
    AUTO_RESPONDER("auto_responder", AutoResponderProcessor())

}