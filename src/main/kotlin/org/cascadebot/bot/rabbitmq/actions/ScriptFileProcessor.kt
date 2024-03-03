package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import net.dv8tion.jda.api.entities.Guild
import org.cascadebot.bot.Main
import org.cascadebot.bot.db.entities.CustomCommandEntity
import org.cascadebot.bot.db.entities.GuildSlotEntity
import org.cascadebot.bot.db.entities.ScriptFileEntity
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.ScriptFileResponse
import org.cascadebot.bot.rabbitmq.objects.UUIDIDObject
import org.cascadebot.bot.utils.QueryUtils.queryJoinedEntities

class ScriptFileProcessor : Processor {

    override fun consume(
        parts: List<String>,
        body: ObjectNode,
        context: RabbitMQContext,
        guild: Guild
    ): RabbitMQResponse<*> {
        /*
        * TODO:
        *  - Get by ID
        *  - Get all for guild
        *  - Create
        *  - Update
        *  - Delete
        * */

        if (parts.isEmpty()) {
            return CommonResponses.UNSUPPORTED_ACTION
        }

        return when {
            checkAction(parts, "get", "byId") -> getScriptFile(body, guild)
            checkAction(parts, "get", "all") -> getAllScriptFiles(body, guild)
            checkAction(parts, "create") -> createScriptFile(body, guild)
            checkAction(parts, "update") -> updateScriptFile(body, guild)
            checkAction(parts, "delete") -> deleteScriptFile(body, guild)
            else -> CommonResponses.UNSUPPORTED_ACTION
        }
    }

    private fun getScriptFile(body: ObjectNode, guild: Guild): RabbitMQResponse<ScriptFileResponse> {
        val idBody = Main.json.treeToValue<UUIDIDObject>(body)

        val scriptFile = dbTransaction {
            get(ScriptFileEntity::class.java, idBody.id)
        }

        if (scriptFile == null || scriptFile.slot.guildId != guild.idLong) {
            return CommonResponses.SCRIPT_FILE_NOT_FOUND
        }

        return RabbitMQResponse.success(ScriptFileResponse.fromEntity(scriptFile))
    }

    private fun getAllScriptFiles(body: ObjectNode, guild: Guild): RabbitMQResponse<List<ScriptFileResponse>> {
        val scriptFiles = dbTransaction {
            queryJoinedEntities(ScriptFileEntity::class.java, GuildSlotEntity::class.java) { _, join ->
                equal(join.get<Long>("guildId"), guild.idLong)
            }.list()
        }

        return RabbitMQResponse.success(scriptFiles.map { ScriptFileResponse.fromEntity(it) })
    }

    private fun createScriptFile(body: ObjectNode, guild: Guild): RabbitMQResponse<ScriptFileResponse> {
        TODO("Not yet implemented")
    }

    private fun updateScriptFile(body: ObjectNode, guild: Guild): RabbitMQResponse<ScriptFileResponse> {
        TODO("Not yet implemented")
    }

    private fun deleteScriptFile(body: ObjectNode, guild: Guild): RabbitMQResponse<ScriptFileResponse> {
        val idBody = Main.json.treeToValue<UUIDIDObject>(body)

        val scriptFile = dbTransaction {
            get(ScriptFileEntity::class.java, idBody.id)
        }

        if (scriptFile == null || scriptFile.slot.guildId != guild.idLong) {
            return CommonResponses.SCRIPT_FILE_NOT_FOUND
        }

        dbTransaction {
            remove(scriptFile)
        }

        return RabbitMQResponse.success(ScriptFileResponse.fromEntity(scriptFile))
    }

}