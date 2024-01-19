package data

import java.time.LocalDateTime
import java.time.Month

enum class Semester {
	WINTER,
	SUMMER;

	companion object {
		fun current() = if (LocalDateTime.now().monthValue in Month.FEBRUARY.value..Month.JULY.value) SUMMER else WINTER

		fun fromString(s: String) = when (s.uppercase()) {
			"W", "WINTER" -> WINTER
			"S", "SUMMER" -> SUMMER
			else -> throw IllegalArgumentException("Invalid semester string")
		}
	}
}