package org.cascadebot.bot.rabbitmq.actions

import org.cascadebot.bot.rabbitmq.actions.channel.TextChannelConsumer

enum class Consumers(val root: String, val consumer: ActionConsumer) {

    GLOBAL("global", GlobalConsumer()),
    USER("user", UserConsumer()),
    ROLE("role", RoleConsumer()),
    TEXT_CHANNEL("channel:text", TextChannelConsumer())

}