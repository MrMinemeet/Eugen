package data

import DatabaseManager
import Kusss
import Util
import java.net.URI

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
	val guildId: Long,
	val courses: List<Course> = Kusss.getCourses(userToken),
	val studentId: Int = -1,
	val exams: List<Exam?> = Kusss.getExams(userToken)
) {
	/**
	 * Creates a new student with the given discord name and calendar token
	 * @param discordName The discord name of the student
	 * @param calendarURI The calendar url of the student
	 */
	constructor(discordName: String, guildId: Long, calendarURI: URI, studentId: Int = -1) :
			this(
				discordName,
				Util.tokenFromURI(calendarURI).orElse(""),
				guildId,
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
			exams.filter { exam -> exam?.lvaNr == it.lvaNr }.forEach { exam -> DatabaseManager.insertExam(exam!!) }
		}
	}

	/**
	 * Creates a new student instance with the given courses removed
	 * @param courses The courses to remove
	 * @return A new student instance with the given courses removed
	 */
	fun removeCourses(vararg courses: Course): Student {
		return Student(
			discordName,
			userToken,
			guildId,
			this.courses - courses.toList().toSet(),
			studentId
		)
	}

	/**
	 * Creates a new student instance with the given courses added
	 * @param courses The courses to add
	 * @return A new student instance with the given courses added
	 */
	fun addCourses(vararg courses: Course): Student {
		return Student(
			discordName,
			userToken,
			guildId,
			this.courses + courses.toList(),
			studentId
		)
	}
}
