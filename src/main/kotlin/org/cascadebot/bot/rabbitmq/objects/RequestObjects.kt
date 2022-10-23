package org.cascadebot.bot.rabbitmq.objects

import com.fasterxml.jackson.annotation.JsonProperty
import net.dv8tion.jda.api.Permission
import org.cascadebot.bot.MessageType
import org.cascadebot.bot.OptionType
import org.cascadebot.bot.ScriptLang
import java.awt.Color
import java.time.Instant
import java.util.UUID

data class UserIDObject(val userId: String)

data class ScriptFile(val id: UUID, val name: String, val code: String)

data class CommandOption<T>(val type: OptionType, val value: T)

data class ExecuteCommandReq(
    val lang: ScriptLang,
    val entrypoint: UUID,
    val files: List<ScriptFile>,
    val options: Map<String, List<CommandOption<Any>>>,
    val member: MemberResponse,
    val channel: ChannelResponse,
    val message: RabbitMqMessage
)
