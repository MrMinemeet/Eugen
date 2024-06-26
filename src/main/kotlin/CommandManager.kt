import data.Exam
import data.Student
import java.awt.Color
import java.net.URI
import java.net.URISyntaxException
import java.sql.SQLException
import java.time.LocalDateTime
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction
import util.*
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.days

const val CATEGORY_NAME: String = "KUSSS"
const val ROLE_NAME: String = "KUSSS"
const val DEL_KUSSS_PREFIX: String = "delete_kusss_"

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
					Logger.warn("URL could not be parsed: ${ex.message}")
					it.hook.sendMessageUserError("Not a valid URI!").queue()
					return@Cmd
				}

				val studentId = try {
					val matNrStr = it.getOption("mat-nr")
					matNrStr?.asInt ?: -1
				} catch (ex: Exception) {
					Logger.warn("MatNr could not be parsed: ${ex.message}")
					it.hook.sendMessageUserError("Not a valid matriculation number!")
					return@Cmd
				}
				val student = try {
					// By constructing a Student-object, the data is added to the database
					val std = Student(it.user.name, it.guild!!.idLong, kusssUri, studentId)
					std.insertIntoDatabase()
					std.assignToCourses()

					std
				} catch (sqlEx: Exception) {
					Logger.error("An error occurred while creating Student: ${sqlEx.message}")
					it.hook.sendMessageBotError("An internal error occurred while creating the database entry!").queue()
					return@Cmd
				}

				try {
					addUserToKusssRole(student)
					addStudentToCourseChannels(student)
				} catch(ex: IllegalStateException) {
					Logger.error("An error occurred while adding user to KUSSS role: ${ex.message}")
					it.hook.sendMessageBotError("An internal error occurred while adding the user to the KUSSS role or while adding to the channels!").queue()
					return@Cmd
				}

				// Sort channels
				Eugen.client.getGuildById(student.guildId)?.let { it1 -> sortChannels(it1) }

				// TODO: Do more
				// After doing stuff, "update" message (can be sent up to 15 min after initial command)
				it.hook.sendMessageOK("You are now subscribed to the Eugen Service").queue()
			},
			OptionData(OptionType.STRING, "url", "URL to the KUSSS calendar", true),
			OptionData(OptionType.INTEGER, "mat-nr", "Your matriculation number", false)
		),

		Cmd("unkusss",
			"Unsubscribe from the Eugen Services",
			{
				it.deferReply().queue() // Show "thinking…"

				val guild = it.guild!!

				// Check if student is even kusssed
				if (DatabaseManager.getStudents().none { s -> s.discordName == it.user.name }) {
					Logger.info("${it.user.name} not kusssed!")
					it.hook.sendMessageUserError("You are not registered to the Eugen Service!").queue()
					return@Cmd
				}


				//remove KUSSS role
				val role = guild.roles.find { role -> role.name == ROLE_NAME }
				if (role != null) {
					guild.removeRoleFromMember(it.user, role).queue()
					Logger.info("Removed user from role ${role.name}")
				} else {
					Logger.info("KUSSS role does not exist on ${guild.name}")
				}

				// Remove from all text channels in category KUSSS
				val category = guild.categories.find { category -> category.name == "KUSSS" }
				category?.textChannels?.forEach { channel ->
					val permissionOverride = it.member?.let { member ->
						channel.getPermissionOverride(member)
					}
					permissionOverride?.manager?.clear(Permission.VIEW_CHANNEL)?.queue()
				}

				// Remove assignment in Database
				DatabaseManager.removeStudentFromAllCourseEnrollments(it.user.name)

				// Remove database entry of user
				DatabaseManager.removeStudent(it.user.name)

				it.hook.sendMessageOK("You are now unsubscribed from my services").queue()
			}),

		Cmd(
			"matnr",
			"Returns the matriculation number for the student",
			{
				it.deferReply().queue()

				// Get StudentID from
				val discordName = try {
					val member =
						it.getOption("user")!!.asMember ?: throw IllegalArgumentException("Could not convert to Member")
					member.user.name
				} catch (ex: IllegalArgumentException) {
					Logger.error("Could not retrieve member: ${ex.message}")
					it.hook.sendMessageBotError("Could not retrieve mentioned user!")
					return@Cmd
				} catch (ex: NullPointerException) {
					Logger.error("Could not retrieve username: ${ex.message}")
					it.hook.sendMessageBotError("An internal error occurred!")
					return@Cmd
				}

				if (discordName == Eugen.client.selfUser.name) {
					it.hook.sendMessageUserError("I don't have a matriculation number. I'm running a successful restaurant")
						.queue()
					return@Cmd
				}

				//Get MatNr
				val matNr = DatabaseManager.getStudentId(discordName)
				if (matNr == -1) {
					it.hook.sendMessageOK("Sorry, I was unable to find a matriculation number for the given user").queue()
				} else {
					it.hook.sendMessageOK("Here is the matriculation number: `$matNr`").queue()
				}
			},
			OptionData(OptionType.USER, "user", "The user to get the matriculation number from", true)
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
						it.hook.sendMessageUserError("You are not my manager!").queue()
						return@Cmd
					}
				} catch (sqlEx: SQLException) {
					Logger.error("Could not retrieve member: ${sqlEx.message}")
					it.hook.sendMessageBotError("Could not update entries!")
					return@Cmd
				}

				it.hook.sendMessageOK("KUSSS entry for ${students.size} students reloaded").queue()
			}
		),

		Cmd("delete-kusss",
			"Removes all LVA channels and the $CATEGORY_NAME",
			{
				if (!Eugen.isManager(it.user.name)) {
					it.replyUserError("You are not my manager!").queue()
					return@Cmd
				}

				// Send message to ask if the user really wants to perform the action
				it.reply("""
					Do you really want to delete all KUSSS-related channels?
					
					🚨 This cannot be undone 🚨
					""".trimIndent())
					.addActionRow(
						Button.danger("${DEL_KUSSS_PREFIX}confirm", Emoji.fromUnicode("\uD83D\uDD25")),
						Button.secondary("${DEL_KUSSS_PREFIX}abort", "Abort")
					).queue()
				return@Cmd
			}
		),

		Cmd(
			"join",
			"Join a course channel you are not enrolled in",
			{
				it.deferReply().queue()

				// Check if user is subscribed to Eugen Service
				val student = getStudentFromName(it.user.name)
				if (student == null) {
					Logger.info("${it.user.name} didn't use /kusss yet. Can't join course channel")
					it.hook.sendMessageUserError("Please use `/kusss` first").queue()
					return@Cmd
				}

				// Student with only the enrolled courses
				val enrolledCourseIds = DatabaseManager.getStudentEnrollment()
					.filter {p -> p.first == student.discordName}
					.map { p -> p.second }
					.toList()
				val actualStudent = Student(
					student.discordName,
					student.discordName,
					student.guildId,
					student.courses.filter { c -> c.lvaNr in enrolledCourseIds },
					student.studentId,
					emptyList()
				)

				// Check if user is already enrolled in the course
				val courseId = it.getOption("course-id")!!.asString.replace(".", "")
				if (DatabaseManager.getStudentEnrollment()
						.any { (dN, cId) -> dN == actualStudent.discordName && cId == courseId }
				) {
					Logger.info("${actualStudent.discordName} already in course with ID $courseId")
					it.hook.sendMessageInfo("You already have access to this course!").queue()
					return@Cmd
				}

				// Check if course exists
				val course = DatabaseManager.getCourses().firstOrNull { c -> c.lvaNr == courseId }
				if (course == null) {
					Logger.warn("Could not find course with ID $courseId")
					it.hook.sendMessageUserError("Could not find course with ID $courseId").queue()
					return@Cmd
				}

				// Add course to student
				val updatedStudent = actualStudent.addCourses(course)
				updatedStudent.assignToCourses()
				addStudentToCourseChannels(updatedStudent)

				Logger.info("Enrolled ${actualStudent.discordName} in course ${course.lvaName}")
				it.hook.sendMessageOK("You joined the course ${course.lvaName}").queue()
			},
			OptionData(OptionType.STRING, "course-id", "The course ID to join. E.g., 123.456", true)
		),

		Cmd(
			"leave",
			"Leave a course channel you joined via /join",
			{
				it.deferReply().queue()
				val guild = it.guild!!

				// Check if user is subscribed to Eugen Service
				val student = getStudentFromName(it.user.name)
				if (student == null) {
					Logger.info("${it.user.name} didn't use /kusss yet. Can't join course channel")
					it.hook.sendMessageUserError("Please use `/kusss` first").queue()
					return@Cmd
				}

				// Student with only the enrolled courses
				val enrolledCourseIds = DatabaseManager.getStudentEnrollment()
					.filter {p -> p.first == student.discordName}
					.map { p -> p.second }
					.toList()
				val actualStudent = Student(
					student.discordName,
					student.discordName,
					student.guildId,
					student.courses.filter { c -> c.lvaNr in enrolledCourseIds },
					student.studentId,
					emptyList()
				)

				// Check if user is even enrolled in the course
				val courseId = it.getOption("course-id")!!.asString.replace(".", "")
				if (DatabaseManager.getStudentEnrollment()
						.none { (dN, cId) -> dN == actualStudent.discordName && cId == courseId }
				) {
					Logger.info("${actualStudent.discordName} not in a course with ID $courseId")
					it.hook.sendMessageInfo("You are not in a course with that id!").queue()
					return@Cmd
				}

				// Get course
				val course = DatabaseManager.getCourses().firstOrNull { c -> c.lvaNr == courseId }
				if (course == null) {
					Logger.info("Could not find course with ID $courseId")
					it.hook.sendMessageUserError("Could not find course with ID $courseId").queue()
					return@Cmd
				}

				// Get course channel
				val channelName = Util.fixLvaName(course.lvaName, true)
				val channel = guild.textChannels.find { c -> c.name == channelName }
				if(channel == null) {
					Logger.info("Could not find channel with name $channelName")
					it.hook.sendMessageBotError("Could not find channel with name $channelName").queue()
					return@Cmd
				}

				// Remove course enrollment from student
				DatabaseManager.removeStudentFromCourseEnrollment(actualStudent, courseId)

				// Remove user from channel
				removeStudentFromCourseChannel(it.member!!, channel)

				Logger.info("Removed ${actualStudent.discordName} from course ${course.lvaName}")
				it.hook.sendMessageOK("You left the course ${course.lvaName}").queue()
			},
			OptionData(OptionType.STRING, "course-id", "The course ID to join. E.g., 123.456", true)
		)
	)

	/**
	 * Is called by [net.dv8tion.jda] when a slash command is received.
	 * Checks if the received command is in [cmdList] and calls the corresponding [Cmd.onEvent] lambda.
	 */
	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		val receivedCommand = event.name
		Logger.info("Received /$receivedCommand")

		// Find a command that matches the received command otherwise return
		val cmd = cmdList.firstOrNull { it.name == receivedCommand }

		if (cmd == null) {
			Logger.warn("No matching cmd found!")
			event.replyBotError("Some internal error occurred!")
			return
		}

		try {
			cmd.onEvent(event)
		} catch (e: Exception) {
			// Catch any other exception and respond if the cmd itself didn't handle it
			event.hook.sendMessageBotError("Something went very wrong!")
		}
	}

	/**
	 * Pass button interactions to the corresponding handler
	 */
	override fun onButtonInteraction(event: ButtonInteractionEvent) {
		when {
			event.componentId.startsWith(DEL_KUSSS_PREFIX) -> handleDeleteKusssButtonInteraction(event)
			else -> Logger.warn("Unknown button interaction!")
		}
	}

	/**
	 * Handles the button interactions for the delete KUSSS button
	 * @param event The button interaction event
	 */
	private fun handleDeleteKusssButtonInteraction(event: ButtonInteractionEvent) {
		if (!Eugen.isManager(event.user.name)) {
			return // No manager, no need to handle this further
		}
		// Drop Buttons
		event.message.editMessageComponents().queue()
		when (event.componentId) {
			"${DEL_KUSSS_PREFIX}confirm" -> {
				val category = event.guild!!.categories.find { category -> category.name == CATEGORY_NAME }
				if (category == null) {
					// Does not exist
					Logger.info("Category $CATEGORY_NAME does not exist")
					event.hook.sendMessageInfo("Category $CATEGORY_NAME does not exist").queue()
					return
				}

				// Remove all channels in category
				category.textChannels.forEach { channel ->
					channel.delete().queue().let { Logger.info("Deleted channel ${channel.name}") }
				}

				// Remove category
				event.editMessage("""
					${StatusEmoji.OK} Deleted category ${category.name} and it's channels
					https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExbDI4cmg1c3J3bThwdGUxaGd2eHJ3dzhoc2h0dXVxZHdrNHBicDM3ZSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/TxlxkVvdWfdRK/giphy.gif
				""".trimMargin()).queue()
				Logger.info("Deleted category ${category.name}")
			}
			"${DEL_KUSSS_PREFIX}abort" -> {
				event.message.editMessageComponents().queue()
				event.editMessage("${StatusEmoji.OK} Aborted deletion of KUSSS-related channels").queue()
				Logger.info("Aborted deletion of KUSSS-related channels")
			}
			else -> {
				Logger.warn("Unknown button interaction for prefix ${DEL_KUSSS_PREFIX}!")
			}
		}
	}

	/**
	 * Guild Command - Instant update but only 100 commands max. Only works when [Eugen.devMode] is true.
	 *
	 * Is called by [net.dv8tion.jda] when a guild is fully ready. So it will be called for each guild.
	 */
	override fun onGuildReady(event: GuildReadyEvent) {
		super.onGuildReady(event)
		Logger.info("${event.guild.name} is ready. Adding ${cmdList.size} commands.")
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

		for(course in student.courses) {
			val channelName = Util.fixLvaName(course.lvaName, true)
			val channel = guild.textChannels
				// Filter for channels in CATEGORY_NAME category and then if the name matches
				.filter { it.parentCategory != null && it.parentCategory!!.name == CATEGORY_NAME }
				.find { it.name.contentEquals(channelName) }
				// If not found, create new channel
				?: createCourseChannel(guild, channelName, course.uri.toString())

			val nextExam = student.exams.firstOrNull { exam -> exam?.lvaNr == course.lvaNr }

			//add next exam to channel topic if exam is in the future, remove exam from channel topic if it is in the past
			if (nextExam != null && nextExam.date.isAfter(LocalDateTime.now())) {
				updateCourseTopic(channel, course.uri.toString(), nextExam)
			} else if(nextExam != null){ //expired exam
				updateCourseTopic(channel, course.uri.toString(), null)
			}

			if (channel.members.contains(guildMember)) {
				// Skip, user is already added to course
				Logger.info("${student.discordName} already assigned to channel ${channel.name}")
			} else {
				// Add user to channel
				channel.manager.putPermissionOverride(
					guildMember,
					listOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
					emptyList()
				).queue().let { Logger.info("Assigned ${student.discordName} to channel ${channel.name}") }
			}
		}

		Logger.info("All channels for ${student.discordName} queued!")
	}

	/**
	 * Removes the user from the course channel.
	 * @param member The member to remove from the channel
	 * @param channel The channel to remove the member from
	 */
	private fun removeStudentFromCourseChannel(member: Member, channel: TextChannel) {
		// Remove user from channel
		channel.manager.putPermissionOverride(
			member,
			emptyList(),
			listOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
		).queue().let {	Logger.info("Removed ${member.nickname} from channel ${channel.name}") }
	}

	/**
	 * Creates a new course channel for a given guild
	 * @param guild The guild to create the channel on
	 * @param name The name of the channel
	 */
	private fun createCourseChannel(guild: Guild, name: String, uri: String = "", nextExam: Exam? = null): TextChannel {
		val category = guild.categories
			.find { it.name == CATEGORY_NAME }
			?: createCategory(guild, CATEGORY_NAME)

		val channelAction = guild.createTextChannel(name)
		channelAction.setParent(category)

		if(nextExam?.date?.isAfter(LocalDateTime.now()) == true) {
			channelAction.setTopic(formatExamTopic(uri, nextExam))
		}

		//set channel private
		channelAction.addPermissionOverride(
			guild.publicRole,
			emptyList(),
			listOf(Permission.VIEW_CHANNEL)
		)

		val channel = channelAction.complete()
		Logger.info("Created channel ${channel.name}")
		return channel
	}
	private fun formatExamTopic(uri: String, nextExam : Exam?) : String {
		if(nextExam == null) {
			return "Links: [KUSSS]($uri)"
		}
		return "Links: [KUSSS]($uri), Next exam: $nextExam"
	}
	private fun updateCourseTopic(channel: TextChannel, uri: String, nextExam : Exam?) {
		val examTopic = formatExamTopic(uri, nextExam);
		if (channel.topic == examTopic) {
			Logger.info("Topic of channel ${channel.name} already up-to-date.")
		} else {
			channel.manager.setTopic(examTopic).queue()
			Logger.info("Updated topic of channel ${channel.name}")
		}
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
		Logger.info("Created category $name on ${guild.name}")
		return category
	}

	/**
	 * Adds the user to the [ROLE_NAME] role
	 * @param student The student to add to the role
	 * @throws IllegalStateException If the guild or role does not exist
	 */
	private fun addUserToKusssRole(student: Student) {
		// Get the guild the user has subscribed on
		val guild = Eugen.client
			.getGuildById(student.guildId)
			?: throw IllegalStateException("Could not find guild by ID ${student.guildId}")

		// Check and create KUSSS role if not existing
		var role = guild.roles.find { it.name == ROLE_NAME }
		if (role == null) {
			Logger.info("Creating $ROLE_NAME role on ${guild.name}")
			val roleAction = guild.createRole()
			roleAction.setName(ROLE_NAME)
			roleAction.setColor(Color.ORANGE)
			role = roleAction.complete()
		}

		if (role == null) {
			throw IllegalStateException("$ROLE_NAME role does not exist on ${guild.name} and could not be created!")
		}
		val user = Eugen.client
			.getUsersByName(student.discordName, true).firstOrNull()
			?: throw IllegalStateException("Could not find user with name '${student.discordName}'")

		val usersWithKusssRole = guild.findMembersWithRoles(role).get()
		if (usersWithKusssRole.any { it.user == user }) {
			Logger.info("${student.discordName} already in role $ROLE_NAME on ${guild.name}")
			return
		}

		guild.addRoleToMember(user, role).queue()
		Logger.info("${student.discordName} added to role $ROLE_NAME on ${guild.name}")
	}

	/**
	 * Reloads all entries from the database and applies changes
	 * @param students The students to reload
	 */
	private fun reloadEntries(students: Array<Student>) {
		if(students.isEmpty()) {
			Logger.info("Nothing to reload!")
			return
		}
		Logger.info("Reloading data for ${students.size} students")
		students.forEach {
			it.assignToCourses()
			addUserToKusssRole(it)
			addStudentToCourseChannels(it)
		}

		// Perform channel sorting for each guild once
		students.map { it.guildId }
			.distinct()
			.mapNotNull { Eugen.client.getGuildById(it) }
			.forEach { sortChannels(it) }
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

		if (channels == sortedChannels) {
			Logger.info("Skipping channel sorting")
			return
		}

		sortedChannels.forEachIndexed { index, channel ->
			channel.manager.setPosition(index).queue().let { Logger.info("Moved ${channel.name} to position $index") }
		}
	}

	/**
	 * Returns the student with the given name
	 * @param name The name of the student
	 * @return The student with the given name. May be null if no student with the given name exists
	 */
	private fun getStudentFromName(name: String) = DatabaseManager.getStudents().find { s -> s.discordName == name }
}