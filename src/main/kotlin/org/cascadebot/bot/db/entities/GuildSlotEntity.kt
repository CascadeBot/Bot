package org.cascadebot.bot.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.cascadebot.bot.SlotType
import org.cascadebot.bot.db.EnumDBType
import org.hibernate.annotations.Type
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "guild_slot")
class GuildSlotEntity(slotType: SlotType, guildId: Long): Serializable {

    constructor() : this(SlotType.TEXT, 0)

    @Id
    @Column(name = "slot_id")
    val slotId: UUID = UUID.randomUUID()

    @Column(name = "slot_type")
    @Type(EnumDBType::class)
    @Enumerated(EnumType.STRING)
    var slotType: SlotType = slotType

    @Column(name = "guild_id")
    var guildId: Long = guildId

    @Column(name = "enabled")
    var enabled: Boolean? = null

    val isCustomCommand
        get() = slotType == SlotType.CUSTOM_CMD || slotType == SlotType.TEXT || slotType == SlotType.PROVIDED

    val isAutoResponder
        get() = slotType == SlotType.AUTO_REPLY

}