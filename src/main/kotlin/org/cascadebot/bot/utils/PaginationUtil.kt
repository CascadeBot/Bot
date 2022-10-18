package org.cascadebot.bot.utils

import net.dv8tion.jda.api.entities.IMentionable
import kotlin.streams.toList

object PaginationUtil {

    fun <T : IMentionable> paginate(list: List<T>, start: Long?, count: Int): PaginationResult<T> {
        val modifiedList = list.stream()
            .sorted { o1, o2 -> o1.idLong.compareTo(o2.idLong) }
            .filter { it.idLong >= (start ?: 0) }.toList()
            .take(count)

        return PaginationResult(
            modifiedList.firstOrNull()?.idLong ?: -1,
            modifiedList.lastOrNull()?.idLong ?: -1,
            modifiedList.size,
            modifiedList
        )
    }

    data class PaginationResult<T : IMentionable>(val start: Long, val end: Long, val count: Int, val items: List<T>)

}