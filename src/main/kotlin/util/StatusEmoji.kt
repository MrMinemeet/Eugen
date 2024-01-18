package util

enum class StatusEmoji (private val emojiCode: String) {
	OK(":white_check_mark:"),
	USER_ERROR(":warning:"),
	BOT_ERROR(":no_entry:");

	override fun toString(): String {
		return emojiCode
	}
}