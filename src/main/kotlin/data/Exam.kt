package data

import java.sql.Date
import java.time.LocalDateTime

data class Exam(
    val lvaNr: String,
    val location : String,
    val date : LocalDateTime,
    val locationId : Int
) {
    override fun toString(): String {
        return "Date: $date, Location: $location"
    }
}


