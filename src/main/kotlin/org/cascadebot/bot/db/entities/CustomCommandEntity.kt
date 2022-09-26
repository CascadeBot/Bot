package org.cascadebot.bot.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.cascadebot.bot.CustomCommandType
import org.cascadebot.bot.ScriptLang
import java.io.Serializable
import java.lang.UnsupportedOperationException
import java.util.UUID

@Entity
@Table(name = "custom_command")
class CustomCommandEntity(slotId: UUID, name: String, customCommandType: CustomCommandType = CustomCommandType.SLASH, lang: ScriptLang): Serializable {

    constructor() : this(UUID.randomUUID(), "", CustomCommandType.SLASH, ScriptLang.JS)

    init {
        if (customCommandType == CustomCommandType.SLASH) {
            throw UnsupportedOperationException("Cannot provide custom command type of slash for this constructor")
        }
    }

    @Id
    @Column(name = "slot_id")
    var slotId: UUID = slotId

    @Column(name = "name")
    var name: String = name

    @Column(name = "description")
    var description: String? = null;

    @Column(name = "marketplace_reference")
    var marketplaceRef: UUID? = null;

    @Column(name = "type")
    var type: CustomCommandType = customCommandType

    @Column(name = "script_lang")
    var lang: ScriptLang = lang

    @Column(name = "entrypoint")
    var entrypoint: UUID? = null

    @OneToMany
    @JoinTable(name = "command_option_join", joinColumns = [JoinColumn(name = "slot_id")], inverseJoinColumns = [JoinColumn(name = "option_id")])
    var options: MutableList<CommandOptionEntity> = mutableListOf()

}