package org.cascadebot.bot.rabbitmq.objects

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ISnowflake
import net.dv8tion.jda.api.entities.PermissionOverride

enum class HolderType {
    ROLE,
    USER
}

enum class PermissionOverrideState {
    ALLOW,
    DENY,
    NEUTRAL
}

data class PermissionOverridePermission(val permission: Permission, val state: PermissionOverrideState)

data class RabbitMqPermissionOverride(val holderId: Long, val holderType: HolderType, val permissions: List<PermissionOverridePermission>): ISnowflake {
    companion object {
        fun fromPermissionOverride(override: PermissionOverride): RabbitMqPermissionOverride {
            val perms: MutableList<PermissionOverridePermission> = mutableListOf()
            for (discordPerm in Permission.values()) {
                var state = PermissionOverrideState.NEUTRAL
                if (override.allowed.contains(discordPerm)) {
                    state = PermissionOverrideState.ALLOW
                }
                if (override.denied.contains(discordPerm)) {
                    state = PermissionOverrideState.DENY
                }
                perms.add(PermissionOverridePermission(discordPerm, state))
            }
            val type = if (override.isMemberOverride) {
                HolderType.USER
            } else {
                HolderType.ROLE
            }
            return RabbitMqPermissionOverride(override.permissionHolder!!.idLong, type, perms)
        }
    }

    override fun getIdLong(): Long {
        return holderId
    }
}