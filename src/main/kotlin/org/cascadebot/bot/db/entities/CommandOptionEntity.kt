package org.cascadebot.bot.db.entities

import com.fasterxml.jackson.databind.JsonNode
import com.vladmihalcea.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
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
        this.name = name;
        this.description = description;
        this.optionType = type;
    }

    constructor(name: String, description: String, entrypoint: UUID): this() {
        this.name = name;
        this.description = description;
        this.optionType = OptionType.SUB_COMMAND;
        this.entrypoint = entrypoint;
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
    var optionType: OptionType = OptionType.STRING

    @Column(name = "constraints")
    @Type(JsonType::class)
    var constraints: JsonNode? = null

    @Column(name = "autocomplete")
    var autocomplete: Boolean? = null

    @Column(name = "entrypoint")
    var entrypoint: UUID? = null

    @OneToMany
    @JoinTable(name = "option_join", joinColumns = [JoinColumn(name = "parent_option")], inverseJoinColumns = [JoinColumn(name = "child_option")])
    var subOptions: MutableList<CommandOptionEntity> = mutableListOf()

}