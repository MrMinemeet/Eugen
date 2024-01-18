package data

import DatabaseManager
import Kusss
import Util
import java.net.URL

/**
 * Represents a student
 * @param discordName The discord name of the student
 * @param userToken The calendar token of the student
 * @param courses The courses of the student (fetched from KUSSS if not set)
 * @param studentId The student id of the student (-1 if not set)
 */
data class Student(
	val discordName: String,
	val userToken: String,
	val courses: List<Course> = Kusss.getCourses(userToken),
	val studentId: Int = -1,
) {
	/**
	 * Creates a new student with the given discord name and calendar token
	 * @param discordName The discord name of the student
	 * @param calendarURL The calendar url of the student
	 */
	constructor(discordName: String, calendarURL: URL, studentId: Int = -1) :
			this(
				discordName,
				Util.tokenFromURL(calendarURL).orElse(""),
				studentId = studentId
			)

	fun insertIntoDatabase() {
		DatabaseManager.insertStudent(this)
	}

	fun assignToCourses() {
		courses.forEach {
			DatabaseManager.insertCourse(it)
			it.assignLecturers()
			DatabaseManager.assignStudentToCourse(this, it)
		}
	}
}
