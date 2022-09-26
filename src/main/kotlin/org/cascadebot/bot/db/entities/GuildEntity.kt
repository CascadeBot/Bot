package org.cascadebot.bot.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "guild")
class GuildEntity(guildId: Long): Serializable {

    constructor() : this(0)

    @Id
    @Column(name = "guild_id")
    val guildId: Long = guildId

}