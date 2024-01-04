package org.example

import java.net.URL
import org.example.data.Student

fun main(args: Array<String>) {
	// ===================== EXAMPLE USAGE OF KUSSS =====================
	// Get URL From Args
	val kusssCalendarURL = URL(args[1])

	// Create Student from URL
	val student = Student(args[0], kusssCalendarURL)
	println(student)

	// ==================================================================
}