import data.Student
import java.net.URI

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

