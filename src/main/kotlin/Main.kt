fun main() {
    val dbConn = DatabaseManager.connection

    Eugen.client.addEventListener(CommandManager())
}

