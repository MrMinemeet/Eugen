package org.example.data

import java.net.URL

/**
 * Represents a course itself
 * @param lvaType The lva type of the course
 * @param lvaNr The lva number of the course
 * @param semester The semester of the course
 * @param lvaName The name of the course
 * @param lecturer The lecturer of the course
 * @param url The url of the course
 */
data class Course(
	val lvaType: LvaType,
	val lvaNr: UInt,
	val semester: Semester,
	val lvaName: String,
	val lecturer: List<String>,
	val url: URL,
)
