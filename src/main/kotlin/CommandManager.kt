import data.Student
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

class CommandManager : ListenerAdapter() {
	private val cmdList = listOf(
		Cmd("sleep",
			"Brings Eugen to a peaceful sleep.",
			{
				it.reply("Going to bed").queue()
				Eugen.client.shutdown()
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
					println("URL could not be parsed:\n${ex.message}")
					it.hook.sendMessage("Not a valid URI!").queue()
					return@Cmd
				}

				val studentId = try {
					val matNrStr = it.getOption("mat-nr")
					matNrStr?.asInt ?: -1
				} catch (ex: Exception) {
					println("MatNr could not be parsed:\n${ex.message}")
					it.hook.sendMessage("Not a valid matrikel number!")
					return@Cmd
				}

				// By constructing a Student-object, the data is added to the database
				Student(it.user.globalName!!, kusssUri.toURL(), studentId)

				// TODO: Do more

				// After doing stuff, "update" message (can be sent up to 15 min after initial command)
				it.hook.sendMessage("Ok :thumbsup:\nYou are now subscribed to the Eugen Service").queue()
			},
			OptionData(OptionType.STRING, "url", "URL to the KUSSS calendar", true),
			OptionData(OptionType.INTEGER, "mat-nr", "Your matrikel number", false)
		)
	)

	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		val receivedCommand = event.name
		println("Received /$receivedCommand")

		// Find a command that matches the received command otherwise return
		val cmd = cmdList.firstOrNull { it.name == receivedCommand }

		if (cmd == null) {
			println("No matching cmd found!")
			event.reply("Some internal error occurred!")
			return
		}

		cmd.onEvent(event)
	}

	// Guild Command - Instant update but only 100 commands max.
	override fun onGuildReady(event: GuildReadyEvent) {
		super.onGuildReady(event)
		println("${event.guild.name} is ready. Adding ${cmdList.size} commands.")
		if (Eugen.devMode)
			registerCommands(event.guild.updateCommands())
	}

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

	private fun registerCommands(clua: CommandListUpdateAction) = clua.addCommands(cmdList.map { it.cmd }).queue()
}