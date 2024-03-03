package org.cascadebot.bot.rabbitmq.actions

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import net.dv8tion.jda.api.entities.Guild
import org.cascadebot.bot.Main
import org.cascadebot.bot.SlotType
import org.cascadebot.bot.db.entities.GuildSlotEntity
import org.cascadebot.bot.db.entities.ScriptFileEntity
import org.cascadebot.bot.rabbitmq.objects.CommonResponses
import org.cascadebot.bot.rabbitmq.objects.CreateScriptFileRequest
import org.cascadebot.bot.rabbitmq.objects.RabbitMQContext
import org.cascadebot.bot.rabbitmq.objects.RabbitMQResponse
import org.cascadebot.bot.rabbitmq.objects.ScriptFileResponse
import org.cascadebot.bot.rabbitmq.objects.UUIDIDObject
import org.cascadebot.bot.rabbitmq.objects.UpdateScriptFileRequest
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
        *  - Create
        *  - Update
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
        val createRequest = Main.json.treeToValue<CreateScriptFileRequest>(body)

        val slot = dbTransaction {
            get(GuildSlotEntity::class.java, createRequest.slotId)
        }

        if (slot == null || slot.guildId != guild.idLong) {
            return CommonResponses.SLOT_NOT_FOUND
        }

        if (slot.slotType != SlotType.CUSTOM_CMD) {
            return CommonResponses.UNSUPPORTED_SLOT
        }

        val scriptFile = ScriptFileEntity(
            slot.slotId,
            createRequest.filename,
            createRequest.script
        )

        dbTransaction {
            persist(scriptFile)
        }

        return RabbitMQResponse.success(ScriptFileResponse.fromEntity(scriptFile))
    }

    private fun updateScriptFile(body: ObjectNode, guild: Guild): RabbitMQResponse<ScriptFileResponse> {
        val updateRequest = Main.json.treeToValue<UpdateScriptFileRequest>(body)

        val scriptFile = dbTransaction {
            get(ScriptFileEntity::class.java, updateRequest.id)
        }

        if (scriptFile == null || scriptFile.slot.guildId != guild.idLong) {
            return CommonResponses.SCRIPT_FILE_NOT_FOUND
        }

        scriptFile.fileName = updateRequest.filename
        scriptFile.script = updateRequest.script

        if (scriptFile.slot.slotId != updateRequest.slotId) {
            val slot = dbTransaction {
                get(GuildSlotEntity::class.java, updateRequest.slotId)
            }
            if (slot == null || slot.guildId != guild.idLong) {
                return CommonResponses.SLOT_NOT_FOUND
            }
            if (slot.slotType != SlotType.CUSTOM_CMD) {
                return CommonResponses.UNSUPPORTED_SLOT
            }
            scriptFile.slot = slot
            scriptFile.slotId = slot.slotId
        }

        dbTransaction {
            persist(scriptFile)
        }

        return RabbitMQResponse.success(ScriptFileResponse.fromEntity(scriptFile))
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