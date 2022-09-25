package org.cascadebot.bot.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.cascadebot.bot.SlotType
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "guild_slot")
class GuildSlot(): Serializable {

    constructor(slotType: SlotType, guildId: Long): this() {
        this.slotType = slotType;
        this.guildId = guildId;
    }

    @Id
    @Column(name = "slot_id")
    var slotId: UUID = UUID.randomUUID() // TODO allow this to be null so postgres can generate it?

    @Column(name = "slot_type")
    var slotType: SlotType = SlotType.PROVIDED

    @Column(name = "guild_id")
    var guildId: Long = 0

    @Column(name = "enabled")
    var enabled: Boolean? = null

}