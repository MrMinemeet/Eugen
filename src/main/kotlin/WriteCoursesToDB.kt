
import java.sql.Connection
import java.sql.DriverManager


var connection: Connection = connectToDB()

fun connectToDB() : Connection {
    return DriverManager.getConnection("jdbc:sqlite:CourseList.db")
}
fun createTable() {

    val statement = connection.createStatement()
    val stmtStr =  "CREATE TABLE IF NOT EXISTS courses (course_id varchar(20) PRIMARY KEY, name varchar(50), " +
            "lecturer varchar(80), semester int)"
    statement.execute(stmtStr)
}

fun insertCourse(id: String, name:String, lecturer:String, semester:String) {
    val stmtStr = "INSERT INTO courses(course_id, name, lecturer, semester) VALUES(?,?,?,?)"

    val stmt = connection.prepareStatement(stmtStr)
    stmt.setString(1, id)
    stmt.setString(2, name)
    stmt.setString(3, lecturer)
    stmt.setString(4, semester)

    stmt.execute()
    println("Added course")
}