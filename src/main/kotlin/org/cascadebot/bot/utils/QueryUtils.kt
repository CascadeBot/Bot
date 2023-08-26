package org.cascadebot.bot.utils

import jakarta.persistence.criteria.Predicate
import org.hibernate.Session
import org.hibernate.query.MutationQuery
import org.hibernate.query.Query
import org.hibernate.query.criteria.HibernateCriteriaBuilder
import org.hibernate.query.criteria.JpaEntityJoin
import org.hibernate.query.criteria.JpaRoot
import org.hibernate.query.sqm.tree.SqmJoinType

object QueryUtils {

    fun <T> Session.getEntities(clazz: Class<T>) : Query<T> {
        val query = criteriaBuilder.createQuery(clazz)
        query.select(query.from(clazz))

        return createQuery(query)
    }

    fun <T> Session.queryEntity(clazz: Class<T>, whereClause: HibernateCriteriaBuilder.(JpaRoot<T>)->Predicate) : Query<T> {
        val query = criteriaBuilder.createQuery(clazz)
        val root = query.from(clazz)
        query.select(root).where(whereClause(criteriaBuilder, root))

        return createQuery(query)
    }

    fun <T, J> Session.queryJoinedEntities(clazz: Class<T>, joiningClazz: Class<J>, whereClause: HibernateCriteriaBuilder.(JpaRoot<T>, JpaEntityJoin<J>) -> Predicate): Query<T> {
        val query = criteriaBuilder.createQuery(clazz)
        val root = query.from(clazz)
        val join = root.join(joiningClazz, SqmJoinType.CROSS)
        query.where(whereClause(criteriaBuilder, root, join))

        return createQuery(query)
    }

    fun <T> Session.deleteEntity(clazz: Class<T>, whereClause: HibernateCriteriaBuilder.(JpaRoot<T>)->Predicate): Int {
        val builder = criteriaBuilder.createCriteriaDelete(clazz)
        builder.where(whereClause(criteriaBuilder, builder.target))

        return createMutationQuery(builder).executeUpdate()
    }

}