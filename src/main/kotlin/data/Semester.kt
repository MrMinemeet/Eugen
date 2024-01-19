package data

import java.time.LocalDateTime
import java.time.Month

enum class Semester {
	WINTER,
	SUMMER;

	companion object {
		fun current() = if (LocalDateTime.now().monthValue in Month.FEBRUARY.value..Month.JULY.value) SUMMER else WINTER
	}
}