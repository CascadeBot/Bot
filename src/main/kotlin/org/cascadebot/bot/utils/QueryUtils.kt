package org.cascadebot.bot.utils

import jakarta.persistence.criteria.Predicate
import org.cascadebot.bot.db.entities.CustomCommandEntity
import org.cascadebot.bot.db.entities.GuildSlotEntity
import org.hibernate.Session
import org.hibernate.query.Query
import org.hibernate.query.criteria.HibernateCriteriaBuilder
import org.hibernate.query.criteria.JpaEntityJoin
import org.hibernate.query.criteria.JpaRoot

object QueryUtils {

    fun <T> Session.getEntities(clazz: Class<T>) : Query<T> {
        val builder = criteriaBuilder
        val query = builder.createQuery(clazz)
        query.select(query.from(clazz))

        return createQuery(query)
    }

    fun <T> Session.queryEntity(clazz: Class<T>, whereClause: HibernateCriteriaBuilder.(JpaRoot<T>)->Predicate) : Query<T> {
        val builder = criteriaBuilder
        val query = builder.createQuery(clazz)
        val root = query.from(clazz)
        query.select(root).where(whereClause(builder, root))

        return createQuery(query)
    }

    fun <T, J> Session.queryJoinedEntities(clazz: Class<T>, joiningClazz: Class<J>, whereClause: HibernateCriteriaBuilder.(JpaRoot<T>, JpaEntityJoin<J>) -> Predicate): Query<T> {
        val builder = criteriaBuilder
        val query = builder.createQuery(clazz)
        val root = query.from(clazz)
        val join = root.join(joiningClazz)
        query.where(whereClause(builder, root, join))

        return createQuery(query)
    }

}