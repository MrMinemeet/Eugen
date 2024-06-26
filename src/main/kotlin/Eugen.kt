import java.io.File
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import util.Logger
import java.io.FileNotFoundException

object Eugen {
	/**
	 * Enable/Disable Dev-Mode
	 * This changes stuff such as usage of guild when enabled and otherwise using global.
	 * Please use this to, e.g. get instant updates on guild server
	 */
	val devMode: Boolean
		get() = true

	val client: JDA = try {
		JDABuilder
			.createDefault(System.getenv("EUGEN_BOT_TOKEN"))
			.setStatus(OnlineStatus.ONLINE)
			.setMemberCachePolicy(MemberCachePolicy.ALL)
			.setActivity(Activity.watching("money"))
			.enableCache(CacheFlag.ONLINE_STATUS)
			.setChunkingFilter(ChunkingFilter.ALL)
			.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
			.build()
	} catch (e: Exception) {
		if (System.getenv("EUGEN_BOT_TOKEN") == null) {
			Logger.error("'EUGEN_BOT_TOKEN' environment variable is not set")
			throw RuntimeException("'EUGEN_BOT_TOKEN' environment variable is not set")
		}

		throw RuntimeException("Failed to start bot", e)
	}

	/// List of managers that are allowed to control crucial functions (e.g, /sleep) of the bot
	private val manager = try {
		File("managers.txt").readLines()
	} catch(e: FileNotFoundException) {
		Logger.warn("No 'managers.txt' file found")
		listOf()
	}

	/**
	 * Checks if the given name is a manager
	 */
	fun isManager(name: String) = manager.contains(name)
}