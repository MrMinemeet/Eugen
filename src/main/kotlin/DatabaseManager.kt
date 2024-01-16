import data.Course
import data.Student
import java.sql.Connection
import java.sql.DriverManager


object DatabaseManager {
	val devMode: Boolean
		get() = true

	private const val DB_FILE_NAME = "CourseList.db"

	val connection: Connection = DriverManager.getConnection("jdbc:sqlite:$DB_FILE_NAME")

	init {
		createCoursesTable()
		createLecturerAssignmentTable()
		createStudentTable()
		createStudentEnrollmentTable()
	}

	private fun createCoursesTable() {
		val statement = connection.createStatement()
		val stmtStr = """
        CREATE TABLE IF NOT EXISTS courses (lvaNr int PRIMARY KEY, lvaType varchar(10), 
        semester varchar(20), lvaName varchar(80), url varchar(100))
    """.trimIndent()
		statement.execute(stmtStr)
	}

	private fun createLecturerAssignmentTable() {
		val statement = connection.createStatement()
		val stmtStr = """
        CREATE TABLE IF NOT EXISTS lecturerAssignment (lvaNr int, lecturer varchar(100), 
        PRIMARY KEY (lvaNr, lecturer), FOREIGN KEY (lvaNr) REFERENCES courses(lvaNr))
    """.trimIndent()
		statement.execute(stmtStr)
	}

	private fun createStudentTable() {
		val statement = connection.createStatement()
		val stmtStr =  "CREATE TABLE IF NOT EXISTS students (userToken varchar(100) PRIMARY KEY, discordName varchar(50))"
		statement.execute(stmtStr)
	}

	//TODO split primary key into two
	private fun createStudentEnrollmentTable() {
		val statement = connection.createStatement()
		val stmtStr = """
        CREATE TABLE IF NOT EXISTS studentEnrollment (userToken varchar(100), course_id varchar(20),
        PRIMARY KEY (userToken, course_id), FOREIGN KEY (userToken) REFERENCES students(userToken), 
        FOREIGN KEY (course_id) REFERENCES courses(course_id))
    """.trimIndent()
		statement.execute(stmtStr)
	}

	fun insertCourse(course : Course, debugOutput: Boolean = false) {
		val lvaType : String = course.lvaType.toString()
		if (debugOutput) println(lvaType)

		val lvaNr : String = course.lvaNr
		if (debugOutput) println(lvaNr)
		val stmtStr = """
        INSERT INTO courses(lvaNr, lvaName, lvaType, semester, url) VALUES(?,?,?,?,?) ON CONFLICT(lvaNr) 
        DO UPDATE SET lvaName=excluded.lvaName, lvaType=excluded.lvaType, semester=excluded.semester, url=excluded.url;
    """.trimIndent()

		val semester : String = course.semester.toString()
		if (debugOutput) println(semester)

		val lvaName : String = course.lvaName
		if (debugOutput) println(lvaName)

		val url : String = course.uri.toString()

		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, lvaNr)
		stmt.setString(2, lvaName)
		stmt.setString(3, lvaType)
		stmt.setString(4, semester)
		stmt.setString(5, url)

		stmt.execute()
	}

	fun insertStudent(student : Student, debugOutput: Boolean = false) {
		val stmtStr = "INSERT INTO students(userToken, discordName) VALUES(?,?) ON CONFLICT(userToken) DO UPDATE SET discordName=excluded.discordName;"

		val discordName : String = student.discordName
		if (debugOutput) println(discordName)

		val userToken : String = student.userToken
		if (debugOutput) println(userToken)

		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, userToken)
		stmt.setString(2, discordName)
		stmt.execute()
	}

	fun assignStudentToCourse(student : Student, course : Course, debugOutput: Boolean = false) {
		val stmtStr = "INSERT INTO studentEnrollment(userToken, course_id) VALUES(?,?) ON CONFLICT(userToken, course_id) DO NOTHING"

		val userToken : String = student.userToken
		if (debugOutput) println(userToken)
		val courseId : String = course.lvaNr

		if (debugOutput) println(courseId)

		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, userToken)
		stmt.setString(2, courseId)
		stmt.execute()
	}

	fun assignLecturerToCourse(course : Course, lecturer : String) {
		val stmtStr = "INSERT INTO lecturerAssignment(lvaNr, lecturer) VALUES(?,?) ON CONFLICT(lvaNr, lecturer) DO NOTHING"

		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, course.lvaNr)
		stmt.setString(2, lecturer)
		stmt.execute()
	}


}