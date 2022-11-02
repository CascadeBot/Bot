package org.cascadebot.bot.db.entities

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.vladmihalcea.hibernate.type.array.ListArrayType
import com.vladmihalcea.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "auto_responder")
class AutoResponderEntity(slotId: UUID, text: JsonNode, match: MutableList<String>): Serializable, SlotEntry {

    constructor() : this(UUID.randomUUID(), ObjectMapper().createObjectNode(), mutableListOf())

    @Id
    @Column(name = "slot_id")
    override var slotId: UUID = slotId

    @Column(name = "text")
    @Type(JsonType::class)
    var text: JsonNode = text

    @Column(name = "match_text")
    @Type(value = ListArrayType::class)
    var match: MutableList<String> = match

}