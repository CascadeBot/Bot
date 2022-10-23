/*
 * Copyright (c) 2022 CascadeBot. All rights reserved.
 * Licensed under the MIT license.
 */

package org.cascadebot.bot.db

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.type.EnumType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class EnumDBType<T : Enum<T>> : EnumType<T>() {

    override fun nullSafeSet(st: PreparedStatement, value: T?, index: Int, session: SharedSessionContractImplementor?) {
        if (value == null) {
            st.setNull(index, Types.OTHER)
        } else {
            st.setObject(index, value.toString(), Types.OTHER)
        }
    }

    override fun nullSafeGet(
        rs: ResultSet,
        position: Int,
        session: SharedSessionContractImplementor,
        owner: Any
    ): T {
        val enum = returnedClass().enumConstants.firstOrNull {
            it.name.equals(rs.getString(position), true)
        }
        requireNotNull(enum) { "Enum type at position $position could not be found in enum class ${returnedClass().name}" }
        return enum
    }

}