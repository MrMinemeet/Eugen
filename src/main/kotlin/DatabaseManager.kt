import data.Course
import data.Student
import java.sql.Connection
import java.sql.DriverManager


object DatabaseManager {
	private val devMode: Boolean
		get() = true

	private const val DB_FILE_NAME = "CourseList.db"

	// Don't make this public, create a function returning the desired data instead
	private val connection: Connection = DriverManager.getConnection("jdbc:sqlite:$DB_FILE_NAME")

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
		val stmtStr =  """
			CREATE TABLE IF NOT EXISTS students (discordName varchar(50) PRIMARY KEY, 
			userToken varchar(100), studentId integer)
		""".trimIndent()
		statement.execute(stmtStr)
	}

	private fun createStudentEnrollmentTable() {
		val statement = connection.createStatement()
		val stmtStr = """
        CREATE TABLE IF NOT EXISTS studentEnrollment (discordName varchar(50), course_id varchar(20),
        PRIMARY KEY (discordName, course_id), FOREIGN KEY (discordName) REFERENCES students(discordName), 
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
		val stmtStr = """
			INSERT INTO students(discordName, userToken, studentId) VALUES(?,?,?) 
			ON CONFLICT(discordName) 
			DO UPDATE SET userToken=excluded.userToken, studentId=excluded.studentId;
		""".trimIndent()

		val discordName : String = student.discordName
		if (debugOutput) println(discordName)

		val userToken : String = student.userToken
		if (debugOutput) println(userToken)

		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, discordName)
		stmt.setString(2, userToken)
		stmt.setInt(3, student.studentId)
		stmt.execute()

		println("Inserted $discordName")
	}

	fun assignStudentToCourse(student : Student, course : Course, debugOutput: Boolean = false) {
		val stmtStr = "INSERT INTO studentEnrollment(discordName, course_id) VALUES(?,?) ON CONFLICT(discordName, course_id) DO NOTHING"

		val discordName : String = student.discordName
		if (debugOutput) println(discordName)

		val courseId : String = course.lvaNr
		if (debugOutput) println(courseId)

		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, discordName)
		stmt.setString(2, courseId)
		stmt.execute()

		println("Assigned $discordName to $courseId")
	}

	fun assignLecturerToCourse(course : Course, lecturer : String) {
		val stmtStr = "INSERT INTO lecturerAssignment(lvaNr, lecturer) VALUES(?,?) ON CONFLICT(lvaNr, lecturer) DO NOTHING"

		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, course.lvaNr)
		stmt.setString(2, lecturer)
		stmt.execute()
	}

	fun getStudentId(discordName : String) : Int {
		val stmtStr = "SELECT studentId FROM students WHERE discordName = ?"
		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, discordName)
		val rs = stmt.executeQuery()
		return rs.getInt("studentId")
	}

	fun getStudents(): Array<Student> {
		val stmtStr = "SELECT discordName, userToken, studentId FROM students"
		val stmt = connection.prepareStatement(stmtStr)
		val rs = stmt.executeQuery()

		val tokens = mutableListOf<Student>()

		while (rs.next()) {
			tokens.add(Student(
				rs.getString("discordName"),
				rs.getString("userToken"),
				studentId = rs.getInt("studentId")
			))
		}

		return tokens.toTypedArray()
	}
	//


}