package org.cascadebot.bot.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "script_file")
class ScriptFileEntity(slotId: UUID, fileName: String, script: String): Serializable {

    constructor() : this(UUID.randomUUID(), "", "")

    @Id
    @Column(name = "script_id")
    var scriptId: UUID? = null

    @Column(name = "slot_id")
    var slotId: UUID = slotId

    @Column(name = "file_name")
    var fileName: String = fileName

    @Column(name = "script")
    var script: String = script

}