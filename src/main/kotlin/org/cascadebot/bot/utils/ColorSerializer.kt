package org.cascadebot.bot.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import okhttp3.internal.toHexString
import java.awt.Color

class ColorSerializer: StdSerializer<Color>(Color::class.java) {
    override fun serialize(value: Color, gen: JsonGenerator, provider: SerializerProvider?) {
        gen.writeStartObject()
        gen.writeNumberField("r", value.red)
        gen.writeNumberField("g", value.green)
        gen.writeNumberField("b", value.blue)
        gen.writeStringField("hex", value.rgb.toHexString())
        gen.writeNumberField("rgb", value.rgb)
        gen.writeEndObject()
    }
}

class ColorDeserializer: StdDeserializer<Color>(Color::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Color {
        val node: JsonNode = p.codec.readTree(p)
        if (node.has("rgb")) {
            return Color(node.get("rgb").asInt())
        } else if (node.has("r") && node.has("g") && node.has("b")) {
            return Color(node.get("r").asInt(), node.get("g").asInt(), node.get("b").asInt())
        } else if (node.has("hex")) {
            return Color.decode(node.get("hex").asText())
        }
        throw UnsupportedOperationException("color didn't have properties needed for deserialization")
    }

}