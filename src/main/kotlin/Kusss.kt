import java.io.StringReader
import java.time.LocalDateTime
import java.time.Month
import java.time.format.DateTimeFormatter
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.CalendarComponent
import data.Course
import data.LvaType
import data.Semester
import data.Session
import java.net.URI
import org.jsoup.Jsoup

object Kusss {

	// ================= Fields
	private val formatter: DateTimeFormatter by lazy { DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss") }

	/** Holds a parsed version of the KUSSS page with all LVAs */
	private val allLVAs: Map<String, Pair<String, URI>> = Jsoup.parse(
		Util.readTextFromURL(URI("https://kusss.jku.at/kusss/coursecatalogue-search-lvas.action?")).trim()
	)
		.select("table")[5] // The 5th table of kusss holds all courses
		.select("tr") // Get all table rows
		.drop(1).associate {  // Get data and add to map
			val courseId = it.select("a")[0]
				.attr("href")
				.trim()
				.substringAfter("showdetails=")
				.substringBefore("&")

			val pair = Pair(
				it.select("b")[1].childNodes()[0].toString(),
				URI(
					"https://kusss.jku.at/kusss/" + it.select("a")[0].attr("href").trim()
				)
			)

			courseId to pair
		}

	// ================= Methods

	/**
	 * Returns a list of all [Course]s and [Session]s for a given user token
	 * @param userToken The user token of the user
	 * @return A list of all [Course]s and [Session]s for the given user token
	 * @throws IllegalArgumentException If the provided token is a URL
	 */
	fun getCoursesAndSessions(
		userToken: String,
		calendar: Calendar? = null
	): Pair<List<Course>, List<Session>> {
		if (userToken.isEmpty() || userToken.contains("http")) throw IllegalArgumentException("Invalid token provided")
		return getCoursesAndSessions(
			URI("https://www.kusss.jku.at/kusss/published-calendar.action?token=${userToken}&lang=de"),
			calendar
		)
	}

	/**
	 * Returns a list of all [Course]s and [Session]s for a given URL
	 * @param uri The URL to get calendar from
	 * @return A list of all [Course]s and [Session]s for the given URL
	 */
	fun getCoursesAndSessions(
		uri: URI,
		calendar: Calendar? = null
	): Pair<List<Course>, List<Session>> {
		// Get/Create calendar here once, in order to not parse multiple times
		val cal = (calendar ?: calendarFromKUSSS(uri))
		return Pair(getCourses(uri, cal), getSessions(uri, cal))
	}

	/**
	 * Returns a list of all [Course]s for a given user token
	 * @param userToken The user token of the user
	 * @return A list of all [Course]s for the given user token
	 * @throws IllegalArgumentException If the provided token is a URL
	 */
	fun getCourses(userToken: String, calendar: Calendar? = null): List<Course> {
		if (userToken.isEmpty() || userToken.contains("http")) throw IllegalArgumentException("Invalid token provided")
		return getCourses(
			URI("https://www.kusss.jku.at/kusss/published-calendar.action?token=${userToken}&lang=de"),
			calendar
		)
	}

	/**
	 * Returns a list of all [Course]s for a given URL
	 * @param uri The URL to get calendar from
	 * @return A list of all [Course]s for the given URL

	 */
	fun getCourses(uri: URI, calendar: Calendar? = null): List<Course> {
		return (calendar ?: calendarFromKUSSS(uri)) // Use passed calendar if possible
			.getComponents<CalendarComponent>()
			.filter { it.name.equals(Component.VEVENT) } // Filter out any other entries that are not a EVENT
			.map { calendarComponentToCourse(it.getProperties()) }
			.distinct()
	}

	/**
	 * Returns a list of all [Session]s for a given user token
	 * @param userToken The user token of the user
	 * @return A list of all [Session]s for the given user token
	 * @throws IllegalArgumentException If the provided token is a URL
	 */
	fun getSessions(userToken: String, calendar: Calendar? = null): List<Session> {
		if (userToken.isEmpty() || userToken.contains("http")) throw IllegalArgumentException("Invalid token provided")
		return getSessions(
			URI("https://www.kusss.jku.at/kusss/published-calendar.action?token=${userToken}&lang=de"),
			calendar
		)
	}

	/**
	 * Returns a list of all [Session]s for a given URL
	 * @param url The URL to get calendar from
	 * @return A list of all [Session]s for the given URL
	 */
	fun getSessions(url: URI, calendar: Calendar? = null): List<Session> {
		return (calendar ?: calendarFromKUSSS(url)) // Use passed calendar if possible
			.getComponents<CalendarComponent>()
			.filter { it.name.equals(Component.VEVENT) } // Filter out any other entries that are not a EVENT
			.map { calendarComponentToSession(it.getProperties()) }
	}

	/**
	 * Converts a CalendarComponent to a [Session]
	 * @param props The properties of the CalendarComponent
	 * @return The converted Session
	 */
	private fun calendarComponentToSession(props: List<Property>): Session {
		// Summary property holds everything important for a "Course" object
		val summary = props[props.indexOfFirst { it.name == Property.SUMMARY }]
			.value.split('/')
		val offset = if (summary.any { it.contains("LVA-Prüfung") }) 1 else 0

		val startString = props[props.indexOfFirst { it.name == Property.DTSTART }].value.trim()
		val endString = props[props.indexOfFirst { it.name == Property.DTEND }].value.trim()

		val descriptionIdx = props.indexOfFirst { it.name == Property.DESCRIPTION }
		val description = if (descriptionIdx != -1) props[descriptionIdx].value.trim() else ""

		val locationIdx = props.indexOfFirst { it.name == Property.LOCATION }
		val location = if (locationIdx != -1) props[locationIdx].value.trim() else ""

		val isExam = summary.any { it.contains("LVA-Prüfung") }.or(description.contains("Klausur"))
			.or(description.contains("Exam"))

		return Session(
			isExam,
			summary[2 + offset].trim().trim('('),
			LocalDateTime.parse(startString, formatter),
			LocalDateTime.parse(endString, formatter),
			description,
			location
		)
	}

	/**
	 * Converts a CalendarComponent to a Course
	 * @param props The properties of the CalendarComponent
	 * @return The converted Course
	 */
	private fun calendarComponentToCourse(props: List<Property>): Course {
		// Summary property holds everything important for a "Course" object
		val summary = props[props.indexOfFirst { it.name == Property.SUMMARY }]
			.value.split('/')
		val offset = if (summary.any { it.contains("LVA-Prüfung") }) 1 else 0

		val lvaNr = summary[2 + offset].trim().trim('(')
		val semester = if (summary[3 + offset].contains("W")) Semester.WINTER else Semester.SUMMER

		val (lvaName, uri) = getLvaKusssInfo(semester, lvaNr, summary[0 + offset].trim())

		// Fetch actual data and return Course object
		return Course(
			LvaType.fromString(summary[0 + offset].trim().split(' ')[0]),
			lvaNr,
			semester,
			lvaName,
			summary[1 + offset].split(',').map { it.trim() },
			uri
		)
	}

	/**
	 * Returns a [Calendar] object from a given KUSSS URL
	 * @param uri The KUSSS URL to parse
	 * @return The parsed [Calendar] object
	 */
	private fun calendarFromKUSSS(uri: URI) = calendarFromIcsString(icsStringFromUrl(uri))

	/**
	 * Returns a [Calendar] object from a given ics string
	 * @param icsString The ics string to parse
	 * @return The parsed [Calendar] object
	 */
	private fun calendarFromIcsString(icsString: String): Calendar = CalendarBuilder().build(StringReader(icsString))

	/**
	 * Returns a [Calendar] object from a given ICS URL
	 * @param url The ics url to parse
	 * @return The parsed [Calendar] object
	 */
	private fun icsStringFromUrl(uri: URI): String = Util.readTextFromURL(uri)

	/**
	 * Fetches the name and description URL from KUSSS
	 * @param semester The semester of the course
	 * @param lvaNr The LVA Nr of the course
	 * @param curLvaName The current LVA Name of the course
	 * @return A Pair of the name and description URL
	 */
	private fun getLvaKusssInfo(semester: Semester, lvaNr: String, curLvaName: String): Pair<String, URI> {
		// If semester != current semester, then just return the current LVA Name.
		// The request only gets the current semester, and the required semester can't be passed via GET
		if (semester != getCurrentSemester())
			return Pair(curLvaName, getUnknownCourseURL(lvaNr))

		// Get course with matching lvaNr
		return allLVAs.getOrDefault(lvaNr, Pair(curLvaName, getUnknownCourseURL(lvaNr)))
	}

	private fun getUnknownCourseURL(lvaNr: String): URI {
		return URI("https://kusss.jku.at/kusss/coursecatalogue-searchlvareg.action?lvasearch=$lvaNr")
	}

	/**
	 * Returns the current semester
	 */
	private fun getCurrentSemester(): Semester {
		// Get current date
		val curDateTime = LocalDateTime.now()

		// Check if month is in [Feb;July]
		return if (curDateTime.monthValue in Month.FEBRUARY.value..Month.JULY.value)
			Semester.SUMMER
		else
			Semester.WINTER
	}
}