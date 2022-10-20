package org.cascadebot.bot.rabbitmq.actions

import org.cascadebot.bot.rabbitmq.actions.channel.TextChannelConsumer

enum class Consumers(val root: String, val consumer: ActionConsumer, val requiresGuild: Boolean) {

    GLOBAL("global", GlobalConsumer(), true),
    USER("user", UserConsumer(), true),
    ROLE("role", RoleConsumer(), true),
    TEXT_CHANNEL("channel:text", TextChannelConsumer(), true)

}