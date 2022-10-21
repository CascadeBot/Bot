package org.cascadebot.bot.rabbitmq.actions

import org.cascadebot.bot.rabbitmq.actions.channel.MessageChannelConsumer

enum class Consumers(val root: String, val consumer: ActionConsumer, val requiresGuild: Boolean) {

    GLOBAL("global", GlobalConsumer(), true),
    USER("user", UserConsumer(), true),
    ROLE("role", RoleConsumer(), true),
    MESSAGE_CHANNEL("channel:message", MessageChannelConsumer(), true)

}