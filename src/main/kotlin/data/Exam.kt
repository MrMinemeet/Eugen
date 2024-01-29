package data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



data class Exam(
    val lvaNr: String,
    val location : String,
    val date : LocalDateTime,
    val locationId : Int = -1
) {
    override fun toString(): String {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm")
        return "Date: ${formatter.format(date)}, Location: $location"
    }
}


