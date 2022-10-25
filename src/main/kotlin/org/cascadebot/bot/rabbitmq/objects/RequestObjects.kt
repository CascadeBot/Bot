package org.cascadebot.bot.rabbitmq.objects

import org.cascadebot.bot.OptionType
import org.cascadebot.bot.ScriptLang
import java.util.UUID

data class UserIDObject(val userId: String)

data class ScriptFileData(val id: UUID, val name: String, val code: String)

data class CommandOption<T>(val type: OptionType, val value: T)

data class ExecCommandRequest(
    val lang: ScriptLang,
    val entrypoint: UUID,
    val files: List<ScriptFileData>,
    val options: Map<String, List<CommandOption<Any>>>,
    val member: MemberResponse,
    val channel: ChannelResponse,
    val interactionId: String
)
