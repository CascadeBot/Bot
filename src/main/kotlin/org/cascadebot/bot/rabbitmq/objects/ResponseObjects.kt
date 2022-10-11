package org.cascadebot.bot.rabbitmq.objects

data class UserResponse(val id: String, val name: String, val avatar: String, val nick: String?, val discrim: String)