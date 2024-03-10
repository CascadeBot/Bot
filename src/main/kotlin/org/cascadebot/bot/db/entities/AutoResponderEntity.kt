package org.cascadebot.bot.db.entities

import com.fasterxml.jackson.databind.JsonNode
import com.vladmihalcea.hibernate.type.array.ListArrayType
import com.vladmihalcea.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.cascadebot.bot.utils.createJsonObject
import org.hibernate.annotations.Type
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "auto_responder")
class AutoResponderEntity(slotId: UUID, text: JsonNode, match: MutableList<String>, enabled: Boolean) : Serializable {

    constructor() : this(UUID.randomUUID(), createJsonObject(), mutableListOf(), false)

    @Id
    @Column(name = "slot_id")
    var slotId: UUID = slotId

    @Column(name = "enabled")
    var enabled: Boolean = enabled

    @Column(name = "text")
    @Type(JsonType::class)
    var text: JsonNode = text

    @Column(name = "match_text")
    @Type(value = ListArrayType::class)
    var match: MutableList<String> = match

    @OneToOne
    @JoinColumn(name = "slot_id", referencedColumnName = "slot_id")
    lateinit var slot: GuildSlotEntity

}