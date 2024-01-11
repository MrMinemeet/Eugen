
import org.example.data.Course
import org.example.data.Student
import java.sql.Connection
import java.sql.DriverManager


var connection: Connection = connectToDB()

fun connectToDB() : Connection {
    return DriverManager.getConnection("jdbc:sqlite:CourseList.db")
}

fun createTables () {
    createCoursesTable()
    createLecturerAssignmentTable()
    createStudentTable()
    createStudentEnrollmentTable()
}

fun createCoursesTable() {
    val statement = connection.createStatement()
    val stmtStr =  "CREATE TABLE IF NOT EXISTS courses (lvaNr int PRIMARY KEY, lvaType varchar(10), " +
            "semester varchar(20), lvaName varchar(80), url varchar(100))"
    statement.execute(stmtStr)
}

fun createLecturerAssignmentTable() {
    val statement = connection.createStatement()
    val stmtStr =  "CREATE TABLE IF NOT EXISTS lecturerAssignment (lvaNr int, lecturer varchar(100), " +
            "PRIMARY KEY (lvaNr, lecturer), FOREIGN KEY (lvaNr) REFERENCES courses(lvaNr))"
    statement.execute(stmtStr)
}

fun createStudentTable() {
    val statement = connection.createStatement()
    val stmtStr =  "CREATE TABLE IF NOT EXISTS students (studentId INTEGER PRIMARY KEY, discordName varchar(50), userToken varchar(50))"
    statement.execute(stmtStr)
}

fun createStudentEnrollmentTable() {
    val statement = connection.createStatement()
    val stmtStr =  "CREATE TABLE IF NOT EXISTS studentEnrollment (studentId int, course_id varchar(20), " +
            "PRIMARY KEY (studentId, course_id), FOREIGN KEY (studentId) REFERENCES students(studentId), " +
            "FOREIGN KEY (course_id) REFERENCES courses(course_id))"
    statement.execute(stmtStr)
}

fun insertCourse(course : Course) {
    val lvaType : String = course.lvaType.toString()
    println(lvaType)

    val lvaNr : String = course.lvaNr
    println(lvaNr)
    val stmtStr = "INSERT INTO courses(lvaNr, lvaName, lvaType, semester, url) VALUES(?,?,?,?,?)"

    val semester : String = course.semester.toString()
    println(semester)

    val lvaName : String = course.lvaName
    println(lvaName)

    val url : String = course.url.toString()

    val stmt = connection.prepareStatement(stmtStr)
    stmt.setString(1, lvaNr)
    stmt.setString(2, lvaName)
    stmt.setString(3, lvaType)
    stmt.setString(4, semester)
    stmt.setString(5, url)

    stmt.execute()
}

fun insertStudent(student : Student) {
    val stmtStr = "INSERT INTO students(discordName, userToken) VALUES(?,?)"

    val discordName : String = student.discordName
    println(discordName)
    val userToken : String = student.userToken
    println(userToken)


    val stmt = connection.prepareStatement(stmtStr)
    stmt.setString(1, discordName)
    stmt.setString(2, userToken)
    stmt.execute()
}

