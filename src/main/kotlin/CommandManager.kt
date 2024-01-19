import data.Student
import java.awt.Color
import java.net.URI
import java.net.URISyntaxException
import java.sql.SQLException
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
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
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.days

const val CATEGORY_NAME: String = "KUSSS"

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

		Cmd(
			"kusss",
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
				} catch (ex: URISyntaxException) {
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
				val student = try {
					// By constructing a Student-object, the data is added to the database
					val std = Student(it.user.globalName!!, it.guild!!.idLong, kusssUri.toURL(), studentId)
					std.insertIntoDatabase()
					std.assignToCourses()

					std
				} catch (sqlEx: Exception) {
					println("An error occurred while creating Student: ${sqlEx.message}")
					it.hook.sendMessageBotError("An internal error occurred while creating the database entry!").queue()
					return@Cmd
				}

				try {
					addUserToKusssRole(student)
					addStudentToCourseChannels(student)
				} catch(ex: IllegalStateException) {
					println("An error occurred while adding user to KUSSS role: ${ex.message}")
					it.hook.sendMessageBotError("An internal error occurred while adding the user to the KUSSS role or while adding to the channels!").queue()
					return@Cmd
				}

				// TODO: Do more
				// After doing stuff, "update" message (can be sent up to 15 min after initial command)
				it.hook.sendMessageOK("You are now subscribed to the Eugen Service").queue()
			},
			OptionData(OptionType.STRING, "url", "URL to the KUSSS calendar", true),
			OptionData(OptionType.INTEGER, "mat-nr", "Your matrikel number", false)
		),

		Cmd("unkusss",
			"Unsubscribe from the Eugen Services",
			{
				it.deferReply().queue() // Show "thinking…"

				// TODO: Delete user-specific data


				//remove KUSSS role
				val role = it.guild?.roles?.find { role -> role.name == "KUSSS" }
				if (role != null) {
					it.guild!!.removeRoleFromMember(it.user, role).queue()
					println("Removed user from role ${role.name}")
				} else {
					error("KUSSS role does not exist")
				}

				// unassign from all text channels in category KUSSS
				val category = it.guild?.categories?.find { category -> category.name == "KUSSS" }
				category?.textChannels?.forEach { channel ->
					val permissionOverride = it.member?.let { member ->
						channel.getPermissionOverride(member)
					}
					permissionOverride?.manager?.clear(Permission.VIEW_CHANNEL)?.queue()
				}

				it.hook.sendMessageOK("You are now unsubscribed from my services").queue()
			}),

		Cmd(
			"matnr",
			"Returns the matrikel number for the student",
			{
				it.deferReply().queue()

				// Get StudentID from
				val discordName = try {
					val member =
						it.getOption("user")!!.asMember ?: throw IllegalArgumentException("Could not convert to Member")
					member.user.globalName!!
				} catch (ex: IllegalArgumentException) {
					println("Could not retrieve member: ${ex.message}")
					it.hook.sendMessageBotError("Could not retrieve mentioned user!")
					return@Cmd
				} catch (ex: NullPointerException) {
					println("Could not retrieve username: ${ex.message}")
					it.hook.sendMessageBotError("An internal error occurred!")
					return@Cmd
				}

				if (discordName == "Eugen") {
					it.hook.sendMessageUserError("I don't have a Matrikel Number. I'm running a successful restaurant")
						.queue()
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
			OptionData(OptionType.USER, "user", "The user to get the matrikel number from", true)
		),

		Cmd(
			"reload",
			"Reloads all calendar entries and applies changes", {
				it.deferReply().queue()
				val students = DatabaseManager.getStudents()
				// Call functions which would also be called on "/kusss". These either create or update stuff
				try {
					if (Eugen.isManager(it.user.name)) {
						reloadEntries(students)
					} else {
						it.replyUserError("You are not my manager!").queue()
					}
				} catch (sqlEx: SQLException) {
					println("Could not retrieve member: ${sqlEx.message}")
					it.hook.sendMessageBotError("Could not update entries!")
					return@Cmd
				}

				it.hook.sendMessageOK("KUSSS entry for ${students.size} students").queue()
			}
		)
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

		// Run separate thread that reloads all data every X time interval
		thread {
			while (true) {
				reloadEntries(DatabaseManager.getStudents())
				// Run once a day
				Thread.sleep(1.days.inWholeMilliseconds)
			}
		}
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

	/**
	 * Adds the user to the course channels.
	 * Creates the channels if they do not exist yet
	 * @param student The student to add to the channels
	 * @throws IllegalStateException If the guild or channel does not exist
	 */
	private fun addStudentToCourseChannels(student: Student) {
		// Get the guild the user has subscribed on
		val guild = Eugen.client.getGuildById(student.guildId)
			?: throw IllegalStateException("Could not find guild by ID ${student.guildId}")

		val guildMember = guild.getMembersByName(student.discordName, true).firstOrNull()
			?: throw IllegalStateException("Could not find user with name '${student.discordName}'")

		for(course in student.courses){
			val channelName = course.lvaName.replace(" ", "-").lowercase()
			val channel = guild.textChannels
				.find { it.name.contentEquals(channelName) }
				?: createCourseChannel(guild, channelName)

			// Add user to channel
			channel.manager.putPermissionOverride(
				guildMember,
				listOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
				emptyList()
			).queue().let {	println("Assigned ${student.discordName} to channel ${channel.name}") }
		}

		sortChannels(guild)

		println("All channels for ${student.discordName} queued!")
	}

	/**
	 * Creates a new course channel for a given guild
	 * @param guild The guild to create the channel on
	 * @param name The name of the channel
	 */
	private fun createCourseChannel(guild: Guild, name: String): TextChannel {
		val channelAction = guild.createTextChannel(name)
		val category = guild.categories
			.find { it.name == CATEGORY_NAME }
			?: createCategory(guild, CATEGORY_NAME)
		channelAction.setParent(category)
		channelAction.addPermissionOverride(
			guild.publicRole,
			emptyList(),
			listOf(Permission.VIEW_CHANNEL)
		)
		//set channel private
		channelAction.addPermissionOverride(
			guild.publicRole,
			emptyList(),
			listOf(Permission.VIEW_CHANNEL)
		)

		val channel = channelAction.complete()
		println("Created channel ${channel.name}")
		return channel
	}

	/**
	 * Creates a new category for a given guild
	 * @param guild The guild to create the category on
	 * @param name The name of the category
	 * @return The created category
	 */
	private fun createCategory(guild: Guild, name: String): Category {
		val categoryAction = guild.createCategory(name)
		val category = categoryAction.complete()
		println("Created category $name on ${guild.name}")
		return category
	}

	/**
	 * Adds the user to the KUSSS role
	 * @param student The student to add to the role
	 * @throws IllegalStateException If the guild or role does not exist
	 */
	private fun addUserToKusssRole(student: Student) {
		// Get the guild the user has subscribed on
		val guild = Eugen.client
			.getGuildById(student.guildId)
			?: throw IllegalStateException("Could not find guild by ID ${student.guildId}")

		// Check and create KUSSS role if not existing
		var role = guild.roles.find { it.name == CATEGORY_NAME }
		if (role == null) {
			println("Creating KUSSS role on ${guild.name}")
			val roleAction = guild.createRole()
			roleAction.setName(CATEGORY_NAME)
			roleAction.setColor(Color.ORANGE)
			roleAction.queue()
			role = roleAction.complete()
		}

		if (role == null) {
			throw IllegalStateException("KUSSS role does not exist on ${guild.name} and could not be created!")
		}
		val user = Eugen.client
			.getUsersByName(student.discordName, true).firstOrNull()
			?: throw IllegalStateException("Could not find user with name '${student.discordName}'")

		guild.addRoleToMember(user, role).queue()
		println("${student.discordName} added to role ${role.name} on ${guild.name}")
	}

	/**
	 * Reloads all entries from the database and applies changes
	 * @param students The students to reload
	 */
	private fun reloadEntries(students: Array<Student>) {
		println("Reloading data for ${students.size} students")
		students.forEach {
			it.assignToCourses()
			addUserToKusssRole(it)
			addStudentToCourseChannels(it)
		}
	}

	/**
	 * Sorts the channels in the [CATEGORY_NAME] in ascending order by their name.
	 * @param guild The guild to sort the channels on
	 * @throws IllegalStateException If the category does not exist
	 */
	private fun sortChannels(guild: Guild) {
		val category = guild.categories
			.find { it.name == CATEGORY_NAME }
			?: throw IllegalStateException("Could not find category $CATEGORY_NAME on ${guild.name}")

		val channels = category.textChannels
		val sortedChannels = channels.sortedBy { it.name }

		sortedChannels.forEachIndexed { index, channel ->
			channel.manager.setPosition(index).queue().let { println("Moved ${channel.name} to position $index") }
		}
	}
}