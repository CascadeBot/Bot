package org.cascadebot.bot.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.cascadebot.bot.Main
import java.math.BigDecimal
import java.math.BigInteger

// Thank you to https://www.reddit.com/r/Kotlin/comments/65pv1f/comment/dgcfrz9/

/**
 * Takes a function [f] and executes it. If the function throws any type of [Exception], null is returned.
 *
 * @param f The function to be executed.
 * @return The return result of [f] or null if the function throws an exception.
 * @author CascadeBot
 */
inline fun <T> tryOrNull(f: () -> T) =
    try {
        f()
    } catch (_: Exception) {
        null
    }

/**
 * Inline helper function to provide a simple way to create an [ObjectNode] using a receiver builder function.
 *
 * @param objectNode Optionally can be passed it to use this function with an existing object.
 * @param builder A receiver function on [ObjectNode] to apply actions to the object node for building.
 * @return The [objectNode] with the applied changes from the [builder].
 * @author CascadeBot
 */
inline fun createJsonObject(
    objectNode: ObjectNode = Main.json.createObjectNode(),
    builder: ObjectNode.() -> Unit
): ObjectNode {
    builder(objectNode)
    return objectNode
}

/**
 * Creates a [ObjectNode] from a [Collection] of [String] and [Any] pairs to use as properties.
 *
 * Each entry in the collection is converted to the appropriate [JsonNode] subtype with the [ObjectNode.replace] method.
 *
 * @param properties The [Collection] of properties to be added to the object.
 * @return An [ObjectNode] with the added [properties].
 * @author CascadeBot
 */
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

/**
 * Creates a [ObjectNode] using each provided pair of [String] and [Any] as properties.
 *
 * @param properties The pairs of properties to be added to the object.
 * @return An [ObjectNode] with the added [properties].
 * @see createJsonObject
 * @author CascadeBot
 */
fun createJsonObject(vararg properties: Pair<String, Any?>): ObjectNode {
    return createJsonObject(properties.toList())
}