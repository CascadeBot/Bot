package org.cascadebot.bot.rabbitmq.actions

import org.cascadebot.bot.rabbitmq.actions.channel.GenericChannelConsumer
import org.cascadebot.bot.rabbitmq.actions.channel.MessageChannelConsumer
import org.cascadebot.bot.rabbitmq.actions.channel.MovableChannelConsumer

enum class Consumers(val root: String, val consumer: ActionConsumer, val requiresGuild: Boolean) {

    GLOBAL("global", GlobalConsumer(), true),
    USER("user", UserConsumer(), true),
    ROLE("role", RoleConsumer(), true),
    MESSAGE_CHANNEL("channel:message", MessageChannelConsumer(), true),
    GENERAL_CHANNEL("channel:general", GenericChannelConsumer(), true),
    MOVABLE_CHANNEL("channel:movable", MovableChannelConsumer(), true)

}