package data

import assignStudentToCourse
import insertCourse
import insertStudent
import java.net.URL
import Kusss
import Util

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
			) {
				insertStudent(this)
				assignToCourses()
			}

	private fun assignToCourses() {
		for (course in courses) {
			insertCourse(course)
			course.assignLecturers()
			assignStudentToCourse(this, course)
		}
	}
}
