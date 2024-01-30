package listeners

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.hooks.EventListener


/**
 * EventListener that is called when a user joins a guild.
 * Sends a private message to the user with information about the bot.
 */
class OnUserJoinEventListener : EventListener {
	override fun onEvent(event: GenericEvent) {
		if (event !is GuildMemberJoinEvent) {
			return
		}
		val user = event.user
		val guild = event.guild

		println("${user.name} joined $guild")

		val message = """
				Hello ${user.name} and welcome to ${guild.name}!
				
				This server uses me to provide the Eugen Service for KUSSS.
				If you want to use it yourself, then just use the command `\kusss` on the server.
			""".trimIndent()

		Util.sendPM(user, message)
	}
}