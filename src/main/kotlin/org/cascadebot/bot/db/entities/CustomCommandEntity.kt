package org.cascadebot.bot.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData
import org.cascadebot.bot.CustomCommandType
import org.cascadebot.bot.ScriptLang
import org.cascadebot.bot.db.EnumDBType
import org.hibernate.annotations.Type
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "custom_command")
class CustomCommandEntity() : Serializable {

    constructor(slotId: UUID, name: String, description: String, lang: ScriptLang) : this() {
        this.slotId = slotId
        this.name = name
        this.description = description
        this.type = CustomCommandType.SLASH
        this.lang = lang
    }

    constructor(slotId: UUID, name: String, customCommandType: CustomCommandType, lang: ScriptLang) : this() {
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
    @Enumerated(EnumType.STRING)
    var type: CustomCommandType = CustomCommandType.SLASH

    @Column(name = "script_lang")
    @Type(EnumDBType::class)
    @Enumerated(EnumType.STRING)
    var lang: ScriptLang = ScriptLang.JS

    @Column(name = "entrypoint")
    var entrypoint: UUID? = null

    @Column(name = "ephemeral")
    var ephemeral: Boolean? = null

    @OneToMany
    @JoinTable(
        name = "command_option_join",
        joinColumns = [JoinColumn(name = "slot_id")],
        inverseJoinColumns = [JoinColumn(name = "option_id")]
    )
    var options: MutableList<CommandOptionEntity> = mutableListOf()

    @OneToOne
    @JoinColumn(name = "slot_id", referencedColumnName = "slot_id")
    lateinit var slot: GuildSlotEntity

    fun toDiscordCommand(): CommandData {
        when (type) {
            CustomCommandType.SLASH -> {
                val data = Commands.slash(name, description ?: "No description")
                options.map { it.toDiscord() }.forEach {
                    when (it) {
                        is SubcommandData -> data.addSubcommands(it)
                        is SubcommandGroupData -> data.addSubcommandGroups(it)
                        is OptionData -> data.addOptions(it)
                        else -> throw IllegalStateException("Unexpected type ${it::class.simpleName}")
                    }
                }
                return data
            }

            CustomCommandType.CONTEXT_USER -> return Commands.user(name)

            CustomCommandType.CONTEXT_MESSAGE -> return Commands.message(name)

            else -> throw IllegalStateException("Unexpected command type $type")
        }
    }

}