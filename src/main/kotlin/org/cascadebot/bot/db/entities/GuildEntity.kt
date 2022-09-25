package org.cascadebot.bot.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "guild")
class GuildEntity(): Serializable {

    constructor(guildId: Long): this() {
        this.guildId = guildId;
    }

    @Id
    @Column(name = "guild_id")
    var guildId: Long = 0

}