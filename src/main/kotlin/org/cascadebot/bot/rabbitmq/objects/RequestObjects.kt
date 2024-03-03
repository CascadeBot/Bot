package org.cascadebot.bot.rabbitmq.objects

import com.fasterxml.jackson.databind.JsonNode
import org.cascadebot.bot.CustomCommandType
import org.cascadebot.bot.OptionType
import org.cascadebot.bot.ScriptLang
import java.util.UUID

data class UUIDIDObject(val id: UUID)

data class DiscordIDObject(val id: String)

data class ScriptFileData(val id: UUID, val name: String, val code: String)

data class CommandOption<T>(val type: OptionType, val value: T)

data class ExecCommandRequest(
    val lang: ScriptLang,
    val entrypoint: UUID,
    val files: List<ScriptFileData>,
    val options: Map<String, List<CommandOption<Any>>>,
    val member: MemberResponse,
    val channel: ChannelResponse,
    val interactionId: String,
    var guildId: String
)

data class CreateAutoResponderRequest(
    val text: JsonNode,
    val matchText: List<String>,
    val enabled: Boolean
)

data class UpdateAutoResponderRequest(
    val slotId: UUID,
    val text: JsonNode,
    val matchText: List<String>,
    val enabled: Boolean
)

data class CreateCustomCommandRequest(
    val name: String,
    val description: String?,
    val marketplaceRef: UUID?,
    val type: CustomCommandType,
    val lang: ScriptLang,
    val ephemeral: Boolean?,
)

data class UpdateCustomCommandRequest(
    val slotId: UUID,
    val name: String,
    val description: String?,
    val marketplaceRef: UUID?,
    val type: CustomCommandType,
    val lang: ScriptLang,
    val entrypoint: UUID?,
    val ephemeral: Boolean?,
)

