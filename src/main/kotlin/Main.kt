package org.example

import createTable
import insertCourse
import org.example.data.Student
import java.net.URI

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main(args: Array<String>) {
    val kusssCalendarURL = URI.create(args[1]).toURL()

    // Create Student from URL
    val student = Student(args[0], kusssCalendarURL)
    println(student)
    createTable()
    insertCourse("1", "AlgoDat 1", "Mike Rotch", "2023W")
}