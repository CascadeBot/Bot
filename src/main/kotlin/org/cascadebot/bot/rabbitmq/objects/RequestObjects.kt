package org.cascadebot.bot.rabbitmq.objects

import com.fasterxml.jackson.annotation.JsonProperty

data class UserIDObject(@JsonProperty("user_id") val userId: String)