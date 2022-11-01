package org.cascadebot.bot.rabbitmq.actions

import org.cascadebot.bot.rabbitmq.actions.channel.ChannelWithThreadsConsumer
import org.cascadebot.bot.rabbitmq.actions.channel.GenericChannelConsumer
import org.cascadebot.bot.rabbitmq.actions.channel.MessageChannelConsumer
import org.cascadebot.bot.rabbitmq.actions.channel.MovableChannelConsumer
import org.cascadebot.bot.rabbitmq.actions.channel.TextChannelConsumer
import org.cascadebot.bot.rabbitmq.actions.channel.VoiceChanelConsumer

enum class Consumers(val root: String, val consumer: ActionConsumer, val requiresGuild: Boolean) {

    GLOBAL("global", GlobalConsumer(), true),
    USER("user", UserConsumer(), true),
    ROLE("role", RoleConsumer(), true),
    MESSAGE_CHANNEL("channel:message", MessageChannelConsumer(), true),
    GENERAL_CHANNEL("channel:general", GenericChannelConsumer(), true),
    MOVABLE_CHANNEL("channel:movable", MovableChannelConsumer(), true),
    THREADED_CHANNEL("channel:threaded", ChannelWithThreadsConsumer(), true),
    INTERACTION("channel:interaction", InteractionConsumer(), true),
    TEXT_CHANNEL("channel:text", TextChannelConsumer(), true),
    VOICE_CHANNEL("channel:voice", VoiceChanelConsumer(), true),
    MESSAGE("message", MessageConsumer(), true)

}