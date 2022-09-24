package org.cascadebot.bot.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "example")
class ExampleEntity(): Serializable {

    constructor(id: Int) : this() {
        this.id = id
    }

    @Id
    @Column(name = "id")
    var id: Int = 0
}