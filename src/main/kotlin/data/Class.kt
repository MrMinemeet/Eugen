package org.example.data

import java.time.LocalDateTime

/**
 * Represents a class at a specific time
 * @param lvaNr The lva number of the class
 * @param semester The semester of the class
 * @param start The start time of the class
 * @param end The end time of the class
 * @param description The description of the class
 * @param location The location of the class
 */
data class Class(
	val lvaNr: UInt,
	val semester: UInt,
	val start: LocalDateTime,
	val end: LocalDateTime,
	val description: String,
	val location: String,
)
