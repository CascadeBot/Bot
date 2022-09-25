package org.cascadebot.bot.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "script_file")
class ScriptFileEntity(): Serializable {

    constructor(slotId: UUID, fileName: String, script: String): this() {
        this.slotId = slotId;
        this.fileName = fileName;
        this.script = script;
    }

    @Id
    @Column(name = "script_id")
    var scriptId: UUID = UUID.randomUUID()

    @Column(name = "slot_id")
    var slotId: UUID = UUID.randomUUID()

    @Column(name = "file_name")
    var fileName: String = ""

    @Column(name = "script")
    var script: String = ""

}