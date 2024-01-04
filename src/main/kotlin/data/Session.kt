package org.example.data

import java.time.LocalDateTime

/**
 * Represents a class at a specific time
 * @param isExam Whether the class is an exam or not
 * @param lvaNr The lva number of the class
 * @param semester The semester of the class
 * @param start The start time of the class
 * @param end The end time of the class
 * @param description The description of the class
 * @param location The location of the class
 */
data class Session(
	val isExam: Boolean,
	val lvaNr: UInt,
	val semester: Semester,
	val start: LocalDateTime,
	val end: LocalDateTime,
	val description: String,
	val location: String,
)
