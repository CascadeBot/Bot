package org.cascadebot.bot.rabbitmq.actions

enum class Consumers(val root: String, val consumer: ActionConsumer) {

    GLOBAL("global", GlobalConsumer()),
    USER("user", UserConsumer()),
    ROLE("role", RoleConsumer())

}