package org.cascadebot.bot.cmd.meta

import net.dv8tion.jda.api.interactions.commands.build.CommandData

abstract class RootCommand(name: String, description: String) : ExecutableCommand(name, description) {

}