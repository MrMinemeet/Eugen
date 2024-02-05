import data.*
import java.io.StringReader
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.CalendarComponent
import org.jsoup.Jsoup
import java.time.LocalDate
import java.util.Date

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
	private fun getCourses(uri: URI, calendar: Calendar? = null): List<Course> {
		val currentSemester = Semester.current()
		//count February as both semesters
		if(LocalDate.now().monthValue == 2) {
			return (calendar ?: calendarFromKUSSS(uri)) // Use passed calendar if possible
				.getComponents<CalendarComponent>()
				.filter { it.name.equals(Component.VEVENT) } // Filter out any other entries that are not a EVENT
				.map { calendarComponentToCourse(it.getProperties()) }
				.distinct()
		} else {
			return (calendar ?: calendarFromKUSSS(uri)) // Use passed calendar if possible
				.getComponents<CalendarComponent>()
				.filter { it.name.equals(Component.VEVENT) } // Filter out any other entries that are not a EVENT
				.map { calendarComponentToCourse(it.getProperties()) }
				.filter { it.semester == currentSemester } // Filter out courses that are not in the current semester
				.distinct()
		}
	}

	fun getExams(userToken: String, calendar: Calendar? = null): List<Exam?> {
		if (userToken.isEmpty() || userToken.contains("http")) throw IllegalArgumentException("Invalid token provided")
		return getExams(
			URI("https://www.kusss.jku.at/kusss/published-calendar.action?token=${userToken}&lang=de"),
			calendar
		)
	}
	fun getExams(userToken: String, calendar: Calendar? = null, course : Course): List<Exam?> {
		if (userToken.isEmpty() || userToken.contains("http")) throw IllegalArgumentException("Invalid token provided")
		return getExams(
			URI("https://www.kusss.jku.at/kusss/published-calendar.action?token=${userToken}&lang=de"),
			calendar
		)
	}
	private fun getExams(uri: URI, calendar: Calendar? = null, course : Course): List<Exam?> {
		return (calendar ?: calendarFromKUSSS(uri)) // Use passed calendar if possible
			.getComponents<CalendarComponent>()
			.filter { it.name.equals(Component.VEVENT) } // Filter out any other entries that are not a EVENT
			.map { calendarComponentToExam(it.getProperties()) }
			.filter { it?.lvaNr == course.lvaNr } // Filter out courses that are not in the current semester
			.distinct()
	}

	private fun getExams(uri: URI, calendar: Calendar? = null): List<Exam?> {
		return (calendar ?: calendarFromKUSSS(uri)) // Use passed calendar if possible
			.getComponents<CalendarComponent>()
			.filter { it.name.equals(Component.VEVENT) } // Filter out any other entries that are not a EVENT
			.map { calendarComponentToExam(it.getProperties()) }
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
			.value.split(" / ")
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
			.value.split(" / ")
		val offset = if (summary.any { it.contains("LVA-Prüfung") }) 1 else 0
		val lvaNrAndSemester = summary[2 + offset].split("/")



		val lvaNr = lvaNrAndSemester[0].trim().trim('(')
		val semester = Semester.fromString(lvaNrAndSemester[1]
			.trim().trim(')')
			.toCharArray().last()
			.toString())

		val (lvaName, uri) = getLvaKusssInfo(lvaNr, summary[0 + offset].trim())

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

	//returns exam if component is exam, returns null if not
	private fun calendarComponentToExam(props: List<Property>): Exam? {
		// Summary property holds everything important for a "Course" object
		val summary = props[props.indexOfFirst { it.name == Property.SUMMARY }]
			.value.split(" / ")
		val offset = if (summary.any { it.contains("LVA-Prüfung") }) 1 else 0

		if(offset == 0) {
			return null
		}
		val lvaNrAndSemester = summary[2 + offset].split("/")

		val lvaNr = lvaNrAndSemester[0].trim().trim('(')

		val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
		val date = LocalDateTime.parse(props[props.indexOfFirst { it.name == Property.DTSTART }].value.trim(), formatter)
		val locationIndex = props.indexOfFirst { it.name == Property.LOCATION }

		val locationName : String
		if(locationIndex != -1) {
			locationName = props[locationIndex].value.trim()
		} else {
			locationName = "Unknown"
		}
		// Fetch actual data and return Course object
		return Exam(lvaNr, locationName, date)
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
	 * @param uri The ics url to parse
	 * @return The parsed [Calendar] object
	 */
	private fun icsStringFromUrl(uri: URI): String = Util.readTextFromURL(uri)

	/**
	 * Fetches the name and description URL from KUSSS
	 * @param lvaNr The LVA Nr of the course
	 * @param curLvaName The current LVA Name of the course
	 * @return A Pair of the name and description URL
	 */
	private fun getLvaKusssInfo(lvaNr: String, curLvaName: String): Pair<String, URI> {
		// Get course with matching lvaNr
		val (name, uri) = allLVAs[lvaNr] ?: return Pair(curLvaName, getUnknownCourseURL(lvaNr))
		val fixedName = fixLVAName(name)

		if (!fixedName.lowercase().contains("special topic")) {
			// Not a special topic, no further processing to do
			return Pair(fixedName, uri)
		}

		// Get actual name from uri
		val courseTable = Jsoup.parse(Util.readTextFromURL(uri))
			.select("table")
			// Select all tables of class "borderbold"
			.select("table.borderbold")
			// Select table that contains an element with the lvaNr. The LvaNr is the text in an "a" href
			.find {
				it.select("a")
					.any { a ->
						a.text()
							.replace(".", "")
							.contains(lvaNr)
					}
			} ?: return Pair(fixedName, uri)

		// Get all rows of table
		val courseTableRows = courseTable
			// Select inner tbody
			.select("tbody")[1]
			// Select "tr" with class "priorityhighlighted", as the desired row is highlighted by KUSSS when querying with the courseclassID (provided from URI)
			.select("tr.priorityhighlighted")
			.drop(1) // Drop the "tr" with the course number

		if(courseTableRows.isEmpty()) return Pair(fixedName, uri)

		// Get the text of the first td
		val courseName = courseTableRows[0].select("td")[0].text()
			.substringAfter("Untertitel:").substringBefore("Zusatzinfo:").trim()

		return Pair(fixLVAName(courseName), uri)
	}

	/**
	 * Fixes some names by replacing "&" with "and" and so on
	 * @param originalName The original name of the course
	 * @return The fixed name of the course
	 */
	private fun fixLVAName(originalName: String): String {
		return originalName
			.replace("&", "and")
			.replace("/", "-")
	}

	private fun getUnknownCourseURL(lvaNr: String): URI {
		return URI("https://kusss.jku.at/kusss/coursecatalogue-searchlvareg.action?lvasearch=$lvaNr")
	}
}