package org.cascadebot.bot.rabbitmq.objects

import com.fasterxml.jackson.annotation.JsonProperty
import net.dv8tion.jda.api.Permission
import org.cascadebot.bot.MessageType
import java.awt.Color
import java.time.Instant

data class UserIDObject(val userId: String)
