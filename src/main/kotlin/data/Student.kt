package org.example.data

/**
 * Represents a student
 * @param discordName The discord name of the student
 * @param calendarToken The calendar token of the student
 * @param courses The courses of the student
 * @param studentId The student id of the student (if set by the user otherwise -1)
 */
data class Student(
	val discordName: String,
	val calendarToken: String,
	val courses: List<Course>,
	val studentId: Int,
) {
	constructor(discordName: String, calendarToken: String, courses: List<Course>): this(discordName, calendarToken, courses, -1)
}
