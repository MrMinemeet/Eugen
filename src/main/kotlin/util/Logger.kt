package util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Simple logger class to log messages with a timestamp
 */
object Logger {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")

    /**
     * Logs an info message
     * @param message The message to log
     */
    fun info(message: String) =
        println("[${LocalDateTime.now().format(formatter)}] - \u2139\uFE0F [INFO] $message")

    /**
     * Logs a warning message
     * @param message The message to log
     */
    fun warn(message: String) =
        println("[${LocalDateTime.now().format(formatter)}] - \u26A0\uFE0F [WARN] $message")

    /**
     * Logs an error message
     * @param message The message to log
     */
    fun error(message: String) =
        println("[${LocalDateTime.now().format(formatter)}] - \u26D4 [ERROR] $message")

    /**
     * Logs a debug message
     * @param message The message to log
     */
    fun debug(message: String) =
        println("[${LocalDateTime.now().format(formatter)}] - \u0001\uF50D [DEBUG] $message")
}