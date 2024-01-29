package data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



data class Exam(
    val lvaNr: String,
    val location : String,
    val date : LocalDateTime,
    val locationId : Int = -1,
    val formatter : DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm")
) {
    override fun toString(): String {
        return "Date: ${formatter.format(date)}, Location: $location"
    }
}


