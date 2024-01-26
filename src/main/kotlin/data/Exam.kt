package data

import java.sql.Date

data class Exam(
    val lvaNr: String,
    val location : String,
    val date : Date,
    val locationId : Int
) {
    override fun toString(): String {
        return "Date: $date, Location: $location"
    }
}


