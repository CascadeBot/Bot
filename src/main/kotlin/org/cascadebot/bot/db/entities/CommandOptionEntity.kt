package org.cascadebot.bot.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.cascadebot.bot.OptionType
import java.io.Serializable
import java.lang.UnsupportedOperationException
import java.util.UUID

@Entity
@Table(name = "command_option")
class CommandOptionEntity(name: String, description: String, type: OptionType): Serializable {

    init {
        if (type == OptionType.SUB_COMMAND) {
            throw UnsupportedOperationException("Must provide entry point if sub command")
        }
    }

    constructor(name: String, description: String, entrypoint: UUID): this() {
        this.name = name;
        this.description = description;
        this.optionType = OptionType.SUB_COMMAND;
        this.entrypoint = entrypoint;
    }

    constructor() : this("", "", OptionType.SUB_COMMAND)

    @Id
    @Column(name = "option_id")
    var optionId: UUID = UUID.randomUUID()

    @Column(name = "name")
    var name: String = name

    @Column(name = "description")
    var description: String = description

    @Column(name = "type")
    var optionType: OptionType = type

    @Column(name = "constraints")
    var constraints: String? = null

    @Column(name = "autocomplete")
    var autocomplete: Boolean? = null

    @Column(name = "entrypoint")
    var entrypoint: UUID? = null

    @OneToMany
    @JoinTable(name = "option_join", joinColumns = [JoinColumn(name = "parent_option")], inverseJoinColumns = [JoinColumn(name = "child_option")])
    var subOptions: MutableList<CommandOptionEntity> = mutableListOf()

}