package org.cascadebot.bot.utils

import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import org.hibernate.Session
import org.hibernate.query.Query
import org.hibernate.query.criteria.HibernateCriteriaBuilder
import org.hibernate.query.criteria.JpaEntityJoin
import org.hibernate.query.criteria.JpaRoot
import org.hibernate.query.sqm.tree.SqmJoinType

object QueryUtils {

    /**
     * Selects all entities with the specified [clazz] with no condition. [clazz] must be a valid Hibernate table.
     *
     * @param clazz The class of the entity to query.
     */
    fun <T> Session.getEntities(clazz: Class<T>): Query<T> {
        val query = criteriaBuilder.createQuery(clazz)
        query.select(query.from(clazz))

        return createQuery(query)
    }

    /**
     * Selects entities of type [clazz] which match the provided [whereClause]. [clazz] must be a valid Hibernate table.
     *
     * [whereClause] has [HibernateCriteriaBuilder] as the receiver with [JpaRoot] provided as a parameter.
     *
     * @param clazz The class of the entity to query.
     * @param whereClause The function which is used to construct a [Predicate] for the [CriteriaQuery.where] function.
     */
    fun <T> Session.queryEntity(
        clazz: Class<T>,
        whereClause: HibernateCriteriaBuilder.(JpaRoot<T>) -> Predicate
    ): Query<T> {
        val query = criteriaBuilder.createQuery(clazz)
        val root = query.from(clazz)
        query.select(root).where(whereClause(criteriaBuilder, root))

        return createQuery(query)
    }

    /**
     * Selects entities of type [clazz] which match the provided [whereClause]. [joiningClazz] is included in the query
     * allowing the [whereClause] to use the joined entity's values in the query. Helpful for querying entities when
     * related fields from another entity are needed.
     *
     * [whereClause] has [HibernateCriteriaBuilder] as the receiver with [JpaRoot] (The root entity) and [JpaEntityJoin]
     * (The joined entity) provided as parameters.
     *
     * @param clazz The class of the entity to query.
     * @param joiningClazz The class of the entity to join to the query.
     * @param whereClause The function which is used to construct a [Predicate] for the [CriteriaQuery.where] function.
     */
    fun <T, J> Session.queryJoinedEntities(
        clazz: Class<T>,
        joiningClazz: Class<J>,
        whereClause: HibernateCriteriaBuilder.(JpaRoot<T>, JpaEntityJoin<J>) -> Predicate
    ): Query<T> {
        val query = criteriaBuilder.createQuery(clazz)
        val root = query.from(clazz)
        val join = root.join(joiningClazz, SqmJoinType.CROSS)
        query.where(whereClause(criteriaBuilder, root, join))

        return createQuery(query)
    }

    /**
     * **Deletes** all entities of type [clazz] which match the condition by the [whereClause].
     *
     * [whereClause] has [HibernateCriteriaBuilder] as the receiver with [JpaRoot] provided as a parameter.
     *
     * @param clazz The class of the entity to delete.
     * @param whereClause The function which is used to construct a [Predicate] for the [CriteriaQuery.where] function.
     */
    fun <T> Session.deleteEntity(clazz: Class<T>, whereClause: HibernateCriteriaBuilder.(JpaRoot<T>)->Predicate): Int {
        val builder = criteriaBuilder.createCriteriaDelete(clazz)
        builder.where(whereClause(criteriaBuilder, builder.target))

        return createMutationQuery(builder).executeUpdate()
    }

}