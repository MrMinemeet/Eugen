
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
    val stmtStr =  "CREATE TABLE IF NOT EXISTS students (userToken varchar(100) PRIMARY KEY, discordName varchar(50))"
    statement.execute(stmtStr)
}

//TODO split primary key into two
fun createStudentEnrollmentTable() {
    val statement = connection.createStatement()
    val stmtStr =  "CREATE TABLE IF NOT EXISTS studentEnrollment (userToken varchar(100), course_id varchar(20), " +
            "PRIMARY KEY (userToken, course_id), FOREIGN KEY (userToken) REFERENCES students(userToken), " +
            "FOREIGN KEY (course_id) REFERENCES courses(course_id))"
    statement.execute(stmtStr)
}

fun insertCourse(course : Course) {
    val lvaType : String = course.lvaType.toString()
    println(lvaType)

    val lvaNr : String = course.lvaNr
    println(lvaNr)
    val stmtStr = "INSERT INTO courses(lvaNr, lvaName, lvaType, semester, url) VALUES(?,?,?,?,?) ON CONFLICT(lvaNr) DO UPDATE SET lvaName=excluded.lvaName, lvaType=excluded.lvaType, semester=excluded.semester, url=excluded.url;"

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
    val stmtStr = "INSERT INTO students(userToken, discordName) VALUES(?,?) ON CONFLICT(userToken) DO UPDATE SET discordName=excluded.discordName;"

    val discordName : String = student.discordName
    println(discordName)
    val userToken : String = student.userToken
    println(userToken)


    val stmt = connection.prepareStatement(stmtStr)
    stmt.setString(1, userToken)
    stmt.setString(2, discordName)
    stmt.execute()
}

fun assignStudentToCourse(student : Student, course : Course) {
    val stmtStr = "INSERT INTO studentEnrollment(userToken, course_id) VALUES(?,?)"

    val userToken : String = student.userToken
    println(userToken)
    val course_id : String = course.lvaNr
    println(course_id)

    val stmt = connection.prepareStatement(stmtStr)
    stmt.setString(1, userToken)
    stmt.setString(2, course_id)
    stmt.execute()
}

fun assignLecturerToCourse(course : Course, lecturer : String) {
    val stmtStr = "INSERT INTO lecturerAssignment(lvaNr, lecturer) VALUES(?,?)"

    val lvaNr : String = course.lvaNr
    println(lvaNr)
    val lecturer : String = lecturer
    println(lecturer)

    val stmt = connection.prepareStatement(stmtStr)
    stmt.setString(1, lvaNr)
    stmt.setString(2, lecturer)
    stmt.execute()
}

