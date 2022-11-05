package org.cascadebot.bot.db.entities

import com.fasterxml.jackson.databind.JsonNode
import com.vladmihalcea.hibernate.type.json.JsonType
import dev.minn.jda.ktx.interactions.commands.Subcommand
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData
import net.dv8tion.jda.api.utils.data.SerializableData
import org.cascadebot.bot.OptionType
import org.cascadebot.bot.db.EnumDBType
import org.hibernate.annotations.Type
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "command_option")
class CommandOptionEntity(): Serializable {

    constructor(name: String, description: String, type: OptionType): this() {
        if (type == OptionType.SUB_COMMAND) {
            throw UnsupportedOperationException("Must provide entry point if sub command")
        }
        this.name = name
        this.description = description
        this.optionType = type
    }

    constructor(name: String, description: String, entrypoint: UUID): this() {
        this.name = name
        this.description = description
        this.optionType = OptionType.SUB_COMMAND
        this.entrypoint = entrypoint
    }

    @Id
    @Column(name = "option_id")
    val optionId: UUID = UUID.randomUUID()

    @Column(name = "name")
    var name: String = ""

    @Column(name = "description")
    var description: String = ""

    @Column(name = "type")
    @Type(EnumDBType::class)
    @Enumerated(EnumType.STRING)
    var optionType: OptionType = OptionType.STRING

    @Column(name = "required")
    var required: Boolean? = false

    @Column(name = "constraints")
    @Type(JsonType::class)
    var constraints: JsonNode? = null

    @Column(name = "autocomplete")
    var autocomplete: Boolean? = null

    @Column(name = "entrypoint")
    var entrypoint: UUID? = null

    @Column(name = "ephemeral")
    var ephemeral: Boolean? = null

    // TODO: Need to handle choices at some point

    @OneToMany
    @JoinTable(
        name = "option_join",
        joinColumns = [JoinColumn(name = "parent_option")],
        inverseJoinColumns = [JoinColumn(name = "child_option")]
    )
    var subOptions: MutableList<CommandOptionEntity> = mutableListOf()

    private fun toDiscordOption(): OptionData {
        return OptionData(
            optionType.jdaOptionType,
            name,
            description,
            required ?: false,
            autocomplete ?: false
        )
    }

    private fun toDiscordSubCommand(): SubcommandData {
        return Subcommand(name, description) {
            subOptions.forEach {
                addOptions(it.toDiscordOption())
            }
        }
    }

    fun toDiscord(): SerializableData {
        return when (optionType) {
            OptionType.SUB_COMMAND -> {
                toDiscordSubCommand()
            }

            OptionType.SUBCOMMAND_GROUP -> {
                val subcommandGroupData = SubcommandGroupData(name, description)
                subOptions.forEach { subCommand ->
                    subcommandGroupData.addSubcommands(subCommand.toDiscordSubCommand())
                }

                subcommandGroupData
            }

            else -> {
                toDiscordOption()
            }
        }
    }

}