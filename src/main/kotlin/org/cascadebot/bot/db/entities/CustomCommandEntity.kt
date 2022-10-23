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
import org.cascadebot.bot.db.EnumDBType
import org.hibernate.annotations.Type
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "custom_command")
class CustomCommandEntity(): Serializable {

    constructor(slotId: UUID, name: String, description: String, lang: ScriptLang): this() {
        this.slotId = slotId
        this.name = name
        this.description = description
        this.type = CustomCommandType.SLASH
        this.lang = lang
    }

    constructor(slotId: UUID, name: String, customCommandType: CustomCommandType, lang: ScriptLang): this() {
        if (customCommandType == CustomCommandType.SLASH) {
            throw UnsupportedOperationException("Cannot provide custom command type of slash for this constructor")
        }
        this.slotId = slotId
        this.name = name
        this.type = customCommandType
        this.lang = lang
    }

    @Id
    @Column(name = "slot_id")
    var slotId: UUID = UUID.randomUUID()
        private set

    @Column(name = "name")
    var name: String = ""

    @Column(name = "description")
    var description: String? = null

    @Column(name = "marketplace_reference")
    var marketplaceRef: UUID? = null

    @Column(name = "type")
    @Type(EnumDBType::class)
    var type: CustomCommandType = CustomCommandType.SLASH

    @Column(name = "script_lang")
    @Type(EnumDBType::class)
    var lang: ScriptLang = ScriptLang.JS

    @Column(name = "entrypoint")
    var entrypoint: UUID? = null

    @Column(name = "ephemeral")
    var ephemeral: Boolean? = null

    @OneToMany
    @JoinTable(name = "command_option_join", joinColumns = [JoinColumn(name = "slot_id")], inverseJoinColumns = [JoinColumn(name = "option_id")])
    var options: MutableList<CommandOptionEntity> = mutableListOf()

}