import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.hours


object Util {
	private val CACHE_TTL: Long = 4.hours.inWholeMilliseconds // 4h of cache time
	private val requestCache: MutableMap<URI, CachedResponse> = mutableMapOf()

	/**
	 * Reads the content of a URI as a string
	 * Utilizes caching with a defined [CACHE_TTL]
	 * @param uri The URI to read from
	 * @return The content of the URI as a string
	 */
	fun readTextFromURL(uri: URI): String {
		if (requestCache.containsKey(uri) &&
			requestCache.getValue(uri).timestamp + CACHE_TTL > System.currentTimeMillis()
		) {
			// Cache was valid -> reuse response
			return requestCache.getValue(uri).response
		}

		// Shrink cache to remove old entries
		thread(start = true, isDaemon = true, name = "Cache Shrinking Thread") {
			shrinkCache()
		}

		// Create new Request, cache it and return response
		val response = makeRequest(uri)
		requestCache[uri] = CachedResponse(response, System.currentTimeMillis())
		return response
	}

	/**
	 * Reads the content of a URI as a string. Doesn't utilize caching
	 * @param uri The URI to read from
	 * @return The content of the URI as a string
	 * @see readTextFromURL
	 */
	private fun makeRequest(uri: URI): String {
		val response = StringBuilder()
		// Read until null is returned// Get ical file from URL
		try {
			val huc = uri.toURL().openConnection() as HttpsURLConnection
			val br = BufferedReader(InputStreamReader(huc.inputStream))

			// Read until null is returned
			var line: String?
			while (br.readLine().also { line = it } != null) {
				response.append(line).append("\n")
			}
		} catch (e: IOException) {
			System.err.println("Failed to get ics content from provided URL")
			System.err.println(e.stackTrace)
		}
		return response.toString()
	}

	/**
	 * Removes all entries from the cache that are older than [CACHE_TTL]
	 */
	private fun shrinkCache() {
		val now = System.currentTimeMillis()
		requestCache.entries.removeIf { it.value.timestamp + CACHE_TTL < now }
	}

	/**
	 * Extracts the user token from a KUSSS Calendar URL
	 * @param url The URL to extract the token from
	 * @return The user token from the URL
	 */
	fun tokenFromURL(url: URL): Optional<String> {
		val matchResult = Regex("token=([^&]+)")
			.find(url.toString()) ?: return Optional.empty()
		val group = matchResult.groups[1] ?: return Optional.empty()
		return Optional.of(group.value)
	}

	/**
	 * Cache entry for a request
	 * @param response The response of the request
	 * @param timestamp The timestamp of the request
	 */
	private data class CachedResponse(val response: String, val timestamp: Long)
}