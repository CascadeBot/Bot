package org.cascadebot.bot.rabbitmq.objects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserIDObject(@SerialName("user_id") val userId: String)