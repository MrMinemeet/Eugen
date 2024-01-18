import data.Student
import net.dv8tion.jda.api.Permission
import java.net.URI
import java.net.URISyntaxException
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction
import util.replyBotError
import util.replyOK
import util.replyUserError
import util.sendMessageBotError
import util.sendMessageOK
import util.sendMessageUserError
import java.awt.Color

class CommandManager : ListenerAdapter() {
	private val cmdList = listOf(
		Cmd("sleep",
			"Brings Eugen to a peaceful sleep.",
			{
				if (!Eugen.isManager(it.user.name)) {
					it.replyUserError("You are not my manager!").queue()
				} else {
					it.replyOK("Going to bed").queue()
					Eugen.client.shutdown() // Gracefully shutdown the bot, but send already queued messages
				}
			}),

		Cmd("kusss",
			"Subscribe to the Eugen Service",
			{
				it.deferReply().queue() // Show "thinking…"
				val kusssUri = try {
					// Force not-null this parameter as required
					val urlStr = it.getOption("url")!!.asString
					if (!urlStr.startsWith("https://www.kusss.jku.at/"))
						throw URISyntaxException(urlStr, "Not a KUSSS URI")
					else
						URI(urlStr)
				} catch(ex: URISyntaxException) {
					println("URL could not be parsed: ${ex.message}")
					it.hook.sendMessageUserError("Not a valid URI!").queue()
					return@Cmd
				}

				val studentId = try {
					val matNrStr = it.getOption("mat-nr")
					matNrStr?.asInt ?: -1
				} catch (ex: Exception) {
					println("MatNr could not be parsed: ${ex.message}")
					it.hook.sendMessageUserError("Not a valid matrikel number!")
					return@Cmd
				}
				val student : Student
				try {
					// By constructing a Student-object, the data is added to the database
				    student = Student(it.user.globalName!!, kusssUri.toURL(), studentId)
                } catch(sqlEx: Exception) {
                    println("An error occurred while creating Student: ${sqlEx.message}")
                    it.hook.sendMessageBotError("An internal error occurred!").queue()
                    return@Cmd
                }

				addUserToKUSSSRole(it)
				addStudentToCourseChannels(student, it)
				// TODO: Do more
				// After doing stuff, "update" message (can be sent up to 15 min after initial command)
				it.hook.sendMessageOK("You are now subscribed to the Eugen Service").queue()
			},
			OptionData(OptionType.STRING, "url", "URL to the KUSSS calendar", true),
			OptionData(OptionType.INTEGER, "mat-nr", "Your matrikel number", false)),

		Cmd("unkusss",
			"Unsubscribe from the Eugen Services",
			{
				it.deferReply().queue() // Show "thinking…"

				// TODO: Delete user-specific data


				//remove KUSSS role
				val role = it.guild?.roles?.find { it.name == "KUSSS" }
				if(role != null) {
					it.guild!!.removeRoleFromMember(it.user, role).queue()
					println("Removed user from role ${role.name}")
				} else {
					error("KUSSS role does not exist")
				}

				//unassign from all text channels in category KUSSS
				val category = it.guild?.categories?.find { it.name == "KUSSS" }
				if (category != null) {
					category.textChannels.forEach { channel ->
						val permissionOverride = it.member?.let { it1 -> channel.getPermissionOverride(it1) }
						if (permissionOverride != null) {
							permissionOverride.manager.clear(Permission.VIEW_CHANNEL).queue()
						}
					}
				}

				it.hook.sendMessageOK("You are now unsubscribed from my services").queue()
			}),

		Cmd("matnr",
			"Returns the matrikel number for the student",
			{
				it.deferReply().queue()

				// Get StudentID from
				val discordName = try {
					val member = it.getOption("user")!!.asMember ?: throw IllegalArgumentException("Could not convert to Member")
					member.user.globalName!!
				} catch(ex: IllegalArgumentException, ) {
					println("Could not retrieve member: ${ex.message}")
					it.hook.sendMessageBotError("Could not retrieve mentioned user!")
					return@Cmd
				} catch (ex: NullPointerException) {
					println("Could not retrieve username: ${ex.message}")
					it.hook.sendMessageBotError("An internal error occurred!")
					return@Cmd
				}

				if (discordName == "Eugen") {
					it.hook.sendMessageUserError("I don't have a Matrikel Number. I'm running a successful restaurant").queue()
					return@Cmd
				}

				// TODO: Query database for discordName in order to get matNr.
				val matNr = DatabaseManager.getStudentId(discordName)



				if (matNr == 0) {
					it.hook.sendMessageOK("Sorry, I was unable to find a Matrikel Number for the given user").queue()
				} else {
					it.hook.sendMessageOK("Here is the Matrikel Number: `$matNr`").queue()
				}
			},
			OptionData(OptionType.USER, "user", "The user to get the matrikel number from", true))
	)

	/**
	 * Is called by [net.dv8tion.jda] when a slash command is received.
	 * Checks if the received command is in [cmdList] and calls the corresponding [Cmd.onEvent] lambda.
	 */
	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		val receivedCommand = event.name
		println("Received /$receivedCommand")

		// Find a command that matches the received command otherwise return
		val cmd = cmdList.firstOrNull { it.name == receivedCommand }

		if (cmd == null) {
			println("No matching cmd found!")
			event.replyBotError("Some internal error occurred!")
			return
		}

		cmd.onEvent(event)
	}

	/**
	 * Guild Command - Instant update but only 100 commands max. Only works when [Eugen.devMode] is true.
	 *
	 * Is called by [net.dv8tion.jda] when a guild is fully ready. So it will be called for each guild.
	 */
	override fun onGuildReady(event: GuildReadyEvent) {
		super.onGuildReady(event)
		println("${event.guild.name} is ready. Adding ${cmdList.size} commands.")
		if (Eugen.devMode)
			registerCommands(event.guild.updateCommands())
	}

	/**
	 * Global Command - Takes up to 1 hour to update but "unlimited" commands. Only works when [Eugen.devMode] is false.
	 *
	 * Is called by [net.dv8tion.jda] when the bot is fully ready.
	 */
	override fun onReady(event: ReadyEvent) {
		super.onReady(event)
		if (!Eugen.devMode)
			registerCommands(Eugen.client.updateCommands())
	}

	inner class Cmd(
		val name: String,
		private val description: String,
		val onEvent: (SlashCommandInteractionEvent) -> Unit,
		vararg optionData: OptionData
	) {
		private val options = optionData.toList()
		val cmd
			get() = Commands.slash(name, description).addOptions(options)
	}

	/**
	 * Registers all commands in [cmdList] to the given [clua]
	 * @param clua The [CommandListUpdateAction] to register the commands to
	 * @return The [CommandListUpdateAction] with the commands added
	 */
	private fun registerCommands(clua: CommandListUpdateAction) = clua.addCommands(cmdList.map { it.cmd }).queue()
	private fun addStudentToCourseChannels(student: Student, it : SlashCommandInteractionEvent) {
		for(course in student.courses) {
			//look for channel
			val channelNameDiscordFormat = course.lvaName.replace(" ", "-").lowercase()
			var channel = it.guild?.textChannels?.find { c -> c.name.contentEquals(channelNameDiscordFormat) }
			if(channel == null) {
				val channelAction = it.guild?.createTextChannel(course.lvaName)
				if (channelAction != null) {
					//create category if it does not exist yet
					var category = it.guild?.categories?.find { it.name == "KUSSS" }
					if(category == null) {
						val categoryAction = it.guild?.createCategory("KUSSS")
						if (categoryAction != null) {
							categoryAction.queue()
							category = categoryAction.complete()
							println("Created category ${category.name}")
						} else
							error("Could not create category")
					}
					channelAction.setParent(category)
					//set channel private
					channelAction.addPermissionOverride(it.guild!!.publicRole, emptyList(), listOf(Permission.VIEW_CHANNEL))
					channelAction.addPermissionOverride(it.member!!, listOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), emptyList())
					channel = channelAction.complete()
					println("Created channel ${channel.name}")
				} else
					error("Could not create channel")
			}
			//allow user to view and post in channel
			if (channel != null) {
				channel.manager.putPermissionOverride(it.member!!, listOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), emptyList()).queue()
				println("Assigned access to channel ${channel.name}")
			} else {
				error("Could not assign access to channel")
			}
		}
	}

	private fun addUserToKUSSSRole(it : SlashCommandInteractionEvent) {
		var role = it.guild?.roles?.find { it.name == "KUSSS" }
		if(role == null) {
			val roleAction = it.guild?.createRole()
			if (roleAction != null) {
				roleAction.setName("KUSSS")
				roleAction.setColor(Color.ORANGE)
				roleAction.queue()
				role = roleAction.complete()
				println("Created role ${role.name}")
			} else
				error("Could not create role")
		}

		if (role != null) {
			it.guild!!.addRoleToMember(it.user, role).queue()
			println("Added user to role ${role.name}")
		} else {
			error("KUSSS role does not exist and could not be created")
		}
	}
}