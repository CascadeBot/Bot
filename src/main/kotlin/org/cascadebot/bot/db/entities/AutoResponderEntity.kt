package org.cascadebot.bot.db.entities

import com.vladmihalcea.hibernate.type.array.ListArrayType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "auto_responder")
class AutoResponderEntity(): Serializable {

    constructor(slotId: UUID, text: String, match: MutableList<String>): this() {
        this.slotId = slotId;
        this.text = text;
        this.match = match;
    }

    @Id
    @Column(name = "slot_id")
    var slotId: UUID = UUID.randomUUID()

    @Column(name = "text")
    var text: String = ""

    @Column(name = "match_text")
    @Type(value = ListArrayType::class)
    var match: MutableList<String> = mutableListOf()

}