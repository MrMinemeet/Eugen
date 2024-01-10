package org.example

import java.net.URL
import org.example.data.Student

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val kusssCalendarURL = URL(args[1])

    // Create Student from URL
    val student = Student(args[0], kusssCalendarURL)
    println(student)
    createTable()
    insertCourse("1", "AlgoDat 1", "Mike Rotch", "2023W")
}