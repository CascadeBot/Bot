package org.cascadebot.bot.utils

import com.fasterxml.jackson.databind.node.ObjectNode
import net.dv8tion.jda.api.entities.IMentionable
import kotlin.streams.toList

object PaginationUtil {

    fun <T : IMentionable> paginate(list: List<T>, params: PaginationParameters): PaginationResult<T> {
        val modifiedList = list.stream()
            .sorted { o1, o2 -> o1.idLong.compareTo(o2.idLong) }
            .filter { it.idLong >= params.start }.toList()
            .take(params.count)

        return PaginationResult(
            modifiedList.firstOrNull()?.idLong ?: -1,
            modifiedList.lastOrNull()?.idLong ?: -1,
            modifiedList.size,
            modifiedList
        )
    }

    data class PaginationResult<T : IMentionable>(val start: Long, val end: Long, val count: Int, val items: List<T>)

    data class PaginationParameters(val start: Long, val count: Int) {

        fun <T : IMentionable> paginate(list: List<T>) {
            paginate(list, this)
        }
    }

    fun parsePaginationParameters(
        jsonInput: ObjectNode,
        countDefault: Int = 50,
        countMax: Int = 100
    ): PaginationParameters {
        val count = jsonInput.get("count")?.let {
            if (it.canConvertToInt()) {
                it.asInt()
            } else {
                null
            }
        } ?: countDefault

        val start = jsonInput.get("start")?.let {
            if (it.canConvertToLong()) {
                it.asLong()
            } else {
                null
            }
        } ?: 0

        require(count > 0) { "Count must be a positive integer greater than 0" }
        require(count <= countMax) { "Count must be less than the maximum allowed ($countMax)" }

        return PaginationParameters(start, count)
    }

}