package org.example


import createTables
import org.example.data.Student
import java.net.URI

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main(args: Array<String>) {
    createTables()
    val kusssCalendarURL = URI.create(args[1]).toURL()

    // Create Student from URL
    val student = Student(args[0], kusssCalendarURL)
    println(student)

    val courses = Kusss.getCourses(student.userToken)
    for (course in courses) {
        println(course)
    }
}

