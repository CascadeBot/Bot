package org.cascadebot.bot.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.cascadebot.bot.Main
import java.math.BigDecimal
import java.math.BigInteger

// Thank you to https://www.reddit.com/r/Kotlin/comments/65pv1f/comment/dgcfrz9/
inline fun <T> tryOrNull(f: () -> T) =
    try {
        f()
    } catch (_: Exception) {
        null
    }

inline fun createJsonObject(
    objectNode: ObjectNode = Main.json.createObjectNode(),
    creator: ObjectNode.() -> Unit
): ObjectNode {
    creator(objectNode)
    return objectNode
}

fun createJsonObject(properties: Collection<Pair<String, Any?>>): ObjectNode {
    val obj = Main.json.createObjectNode()
    for ((key, value) in properties) {
        val node: JsonNode = when (value) {
            null -> obj.nullNode()
            is Int -> obj.numberNode(value)
            is Long -> obj.numberNode(value)
            is Float -> obj.numberNode(value)
            is Double -> obj.numberNode(value)
            is BigInteger -> obj.numberNode(value)
            is BigDecimal -> obj.numberNode(value)
            is Short -> obj.numberNode(value)
            is ByteArray -> obj.binaryNode(value)
            is Boolean -> obj.booleanNode(value)
            is JsonNode -> value
            is String -> obj.textNode(value)
            else -> obj.pojoNode(value)
        }
        obj.replace(key, node)
    }
    return obj
}

fun createJsonObject(vararg properties: Pair<String, Any?>): ObjectNode {
    return createJsonObject(properties.toList())
}