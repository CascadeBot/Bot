package org.cascadebot.bot.cmd.meta

import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion
import net.dv8tion.jda.api.interactions.commands.OptionMapping

@JvmInline
value class CommandArgs(private val internal: Map<String, List<OptionMapping>>) {
    
    //region primitives
    fun getArgAsLong(name: String): Long? {
        return internal[name]?.firstOrNull()?.asLong
    }

    fun getArgsAsLong(name: String): List<Long> {
        return internal[name]?.map { it.asLong } ?: listOf()
    }

    fun getArgAsBoolean(name: String): Boolean? {
        return internal[name]?.firstOrNull()?.asBoolean
    }

    fun getArgsAsBoolean(name: String): List<Boolean>? {
        return internal[name]?.map { it.asBoolean }
    }

    fun getArgAsString(name: String): String? {
        return internal[name]?.firstOrNull()?.asString
    }

    fun getArgsAsString(name: String): List<String>? {
        return internal[name]?.map { it.asString }
    }
    //endregion
    
    //region discord
    fun getArgAsRole(name: String): Role? {
        return internal[name]?.firstOrNull()?.asRole
    }

    fun getArgsAsRole(name: String): List<Role>? {
        return internal[name]?.map { it.asRole }
    }

    fun getArgAsMessageChannel(name: String): GuildMessageChannel? {
        val arg = internal[name]?.firstOrNull() ?: return null
        return if(arg.asChannel.type.run { isMessage && isGuild }) {
            arg.asChannel.asGuildMessageChannel()
        } else {
            null
        }
    }

    fun getArgsAsMessageChannel(name: String): List<MessageChannel>? {
        return internal[name]?.mapNotNull {
            if(it.asChannel.type.run { isMessage && isGuild }) {
                it.asChannel.asGuildMessageChannel()
            } else {
                null
            }
        }
    }
    
    fun getArgAsGuildChannel(name: String): GuildChannelUnion? {
        return internal[name]?.firstOrNull()?.asChannel
    }

    fun getArgsAsGuildChannel(name: String): List<GuildChannelUnion>? {
        return internal[name]?.map { it.asChannel }
    }

    fun getArgAsMember(name: String): Member? {
        return internal[name]?.firstOrNull()?.asMember
    }

    fun getArgsAsMember(name: String): List<Member>? {
        return internal[name]?.mapNotNull { it.asMember }
    }

    fun getArgAsUser(name: String): User? {
        return internal[name]?.firstOrNull()?.asUser
    }

    fun getArgsAsUser(name: String): List<User> {
        return internal[name]?.map { it.asUser } ?: listOf()
    }

    fun getArgAsMentionable(name: String): IMentionable? {
        return internal[name]?.firstOrNull()?.asMentionable
    }

    fun getArgsAsMentionable(name: String): List<IMentionable> {
        return internal[name]?.map { it.asMentionable } ?: listOf()
    }
    //endregion
    
    

}