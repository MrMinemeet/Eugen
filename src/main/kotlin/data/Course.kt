package data

import DatabaseManager.assignLecturerToCourse
import java.net.URI
import data.LvaType
import data.Semester

/**
 * Represents a course itself
 * @param lvaType The lva type of the course
 * @param lvaNr The lva number of the course
 * @param semester The semester of the course
 * @param lvaName The name of the course
 * @param lecturer The lecturer of the course
 * @param uri The url of the course
 */
data class Course(
	val lvaType: LvaType,
	val lvaNr: String,
	val semester: Semester,
	val lvaName: String,
	val lecturer: List<String>,
	val uri: URI,
) {
	fun assignLecturers() {
		for (l in this.lecturer) {
			assignLecturerToCourse(this, l)
		}
	}
}
