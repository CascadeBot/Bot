package org.cascadebot.bot.rabbitmq.actions

import jakarta.persistence.Table
import org.cascadebot.bot.Main
import org.reflections.Reflections
import java.util.UUID

class DataConsumer {

    fun test() {
        val name = "guild_slot"
        val key = "2d061887-b0a2-40b7-b6b1-7a42f1a00c20"

        val table = Reflections("org.cascadebot.bot.db.entities")
            .getTypesAnnotatedWith(Table::class.java)
            .filter { it.getDeclaredAnnotation(Table::class.java)?.name == name }
            .filterNotNull()
            .firstOrNull()

        if (table == null) return

        val value = Main.postgresManager.transaction {
            get(table, UUID.fromString(key))
        }

        val updatedValue: Any = Main.json.readerForUpdating(value).readValue("""{"enabled": false}""")
        println(updatedValue)
    }

}