package org.example

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

object Util {
	/**
	 * Reads the content of a URL as a string
	 * @param url The URL to read from
	 * @return The content of the URL as a string
	 * @throws IOException If an error occurs while reading from the URL
	 */
	fun readTextFromURL(url: URI): String {
		val response = StringBuilder()
		// Read until null is returned// Get ical file from URL
		try {
			val huc = url.toURL().openConnection() as HttpsURLConnection
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
}