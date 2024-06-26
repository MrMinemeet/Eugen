import data.*
import util.Logger
import java.net.URI
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
		createLocationTable()
		createExamTable()
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
			CREATE TABLE IF NOT EXISTS students (discordName varchar(50) PRIMARY KEY, guildId BIGINT,
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

	private fun createExamTable() {
		val statement = connection.createStatement()
		val stmtStr = """
		CREATE TABLE IF NOT EXISTS exams (examId int PRIMARY KEY, lvaNr int, date text, 
		time text, locationId int, FOREIGN KEY (lvaNr) REFERENCES courses(lvaNr), FOREIGN KEY (locationId) REFERENCES locations(locationId))
	""".trimIndent()
		statement.execute(stmtStr)
	}

	private fun createLocationTable() {
		val statement = connection.createStatement()
		val stmtStr = """
		CREATE TABLE IF NOT EXISTS locations (locationId int PRIMARY KEY, name text)
	""".trimIndent()
		statement.execute(stmtStr)
	}

	fun insertExam(exam : Exam) {
		val stmtStr = """
		INSERT INTO exams(lvaNr, date, time, locationId) VALUES(?,?,?,?) ON CONFLICT(examId) 
		DO UPDATE SET lvaNr=excluded.lvaNr, date=excluded.date, time=excluded.time, locationId=excluded.locationId;
	""".trimIndent()
		val weekDay : String = exam.date.toLocalDate().dayOfWeek.toString()
		val day : String = exam.date.toLocalDate().dayOfMonth.toString()
		val month : String = exam.date.toLocalDate().month.toString()
		val year : String = exam.date.toLocalDate().year.toString()
		val time : String = exam.date.hour.toString() + ":" + exam.date.minute.toString()
		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, exam.lvaNr)
		stmt.setString(2, "$weekDay, $day.$month.$year")
		stmt.setString(3, time)
		stmt.setInt(4, exam.locationId)
		stmt.execute()
	}

	fun insertLocation(location : Location) {
		val stmtStr = """
		INSERT INTO locations(name) VALUES(?) ON CONFLICT(locationId) 
		DO UPDATE SET name=excluded.name;
	""".trimIndent()
		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, location.name)
		stmt.execute()

		val getIDStmtStr = """
			SELECT locationId FROM locations WHERE name = ?
		""".trimIndent()

		val getIDStmt = connection.prepareStatement(getIDStmtStr)
		getIDStmt.setString(1, location.name)
		val rs = getIDStmt.executeQuery()
		location.id = rs.getInt("locationId")
	}

	fun insertCourse(course : Course, debugOutput: Boolean = false) {
		val lvaType : String = course.lvaType.toString()
		if (debugOutput) Logger.debug(lvaType)

		val lvaNr : String = course.lvaNr
		if (debugOutput) Logger.debug(lvaNr)
		val stmtStr = """
        INSERT INTO courses(lvaNr, lvaName, lvaType, semester, url) VALUES(?,?,?,?,?) ON CONFLICT(lvaNr) 
        DO UPDATE SET lvaName=excluded.lvaName, lvaType=excluded.lvaType, semester=excluded.semester, url=excluded.url;
    """.trimIndent()

		val semester : String = course.semester.toString()
		if (debugOutput) Logger.debug(semester)

		val lvaName : String = course.lvaName
		if (debugOutput) Logger.debug(lvaName)

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
			INSERT INTO students(discordName, guildId, userToken, studentId) VALUES(?,?,?,?) 
			ON CONFLICT(discordName) 
			DO UPDATE SET guildId=excluded.guildId, userToken=excluded.userToken, studentId=excluded.studentId;
		""".trimIndent()

		val discordName: String = student.discordName
		if (debugOutput) Logger.debug(discordName)

		val guildId: Long = student.guildId
		if (debugOutput) Logger.debug(guildId.toString())

		val userToken: String = student.userToken
		if (debugOutput) Logger.debug(userToken)

		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, discordName)
		stmt.setLong(2, guildId)
		stmt.setString(3, userToken)
		stmt.setInt(4, student.studentId)
		stmt.execute()

		Logger.info("Inserted $discordName")
	}

	fun removeStudent(discordName: String) {
		val stmtStr = "DELETE FROM students WHERE discordName = ?"
		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, discordName)
		stmt.execute()

		Logger.info("Removed user from database")
	}

	fun assignStudentToCourse(student : Student, course : Course, debugOutput: Boolean = false) {
		val stmtStr = "INSERT INTO studentEnrollment(discordName, course_id) VALUES(?,?) ON CONFLICT(discordName, course_id) DO NOTHING"

		val discordName : String = student.discordName
		if (debugOutput) Logger.debug(discordName)

		val courseId : String = course.lvaNr
		if (debugOutput) Logger.debug(courseId)

		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, discordName)
		stmt.setString(2, courseId)
		stmt.execute()

		Logger.info("Assigned $discordName to $courseId")
	}

	fun removeStudentFromAllCourseEnrollments(discordName: String) {
		val stmtStr = "DELETE FROM studentEnrollment WHERE discordName = ?"
		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, discordName)
		stmt.execute()
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

	fun getCourses(): Array<Course> {
		val stmtStr = """
			SELECT courses.lvaNr, lvaType, semester, lvaName, url, lecturer FROM courses
			LEFT JOIN lecturerAssignment ON courses.lvaNr = lecturerAssignment.lvaNr
		""".trimIndent()
		val stmt = connection.prepareStatement(stmtStr)
		val rs = stmt.executeQuery()
		val tmpCourses = mutableListOf<Course>()

		val lecturerMap = mutableMapOf<String, MutableList<String>>()

		while (rs.next()) {
			tmpCourses.add(Course(
				LvaType.fromString(rs.getString("lvaType")),
				rs.getString("lvaNr"),
				Semester.fromString(rs.getString("semester")),
				rs.getString("lvaName"),
				emptyList(),
				URI(rs.getString("url"))
			))

			val lecturer = rs.getString("lecturer")
			if(lecturer != null) {
				if(lecturerMap.containsKey(rs.getString("lvaNr"))) {
					lecturerMap[rs.getString("lvaNr")]!!.add(lecturer)
				} else {
					lecturerMap[rs.getString("lvaNr")] = mutableListOf(lecturer)
				}
			}
		}

		val courses = mutableListOf<Course>()
		tmpCourses.forEach {
			courses.add(Course(
				it.lvaType,
				it.lvaNr,
				it.semester,
				it.lvaName,
				lecturerMap[it.lvaNr] ?: emptyList(),
				it.uri
			))
		}

		return courses.toTypedArray()
	}

	fun getStudents(): Array<Student> {
		val stmtStr = "SELECT * FROM students"
		val stmt = connection.prepareStatement(stmtStr)
		val rs = stmt.executeQuery()

		val students = mutableListOf<Student>()

		while (rs.next()) {
			students.add(Student(
				rs.getString("discordName"),
				rs.getString("userToken"),
				rs.getLong("guildId"),
				studentId = rs.getInt("studentId")
			))
		}

		return students.toTypedArray()
	}

	fun getStudentEnrollment(): Array<Pair<String, String>> {
		val stmtStr = "SELECT * FROM studentEnrollment"
		val stmt = connection.prepareStatement(stmtStr)
		val rs = stmt.executeQuery()

		val studentEnrollment = mutableListOf<Pair<String, String>>()

		while (rs.next()) {
			studentEnrollment.add(Pair(
				rs.getString("discordName"),
				rs.getString("course_id")
			))
		}

		return studentEnrollment.toTypedArray()
	}

	fun removeStudentFromCourseEnrollment(student: Student, courseId: String) {
		val stmtStr = "DELETE FROM studentEnrollment WHERE discordName = ? AND course_id = ?"
		val stmt = connection.prepareStatement(stmtStr)
		stmt.setString(1, student.discordName)
		stmt.setString(2, courseId)
		stmt.execute()
	}
}