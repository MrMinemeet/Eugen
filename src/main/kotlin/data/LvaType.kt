package data

enum class LvaType {
	VL,
	VO,
	UE,
	KV,
	PR,
	KS,
	Unknown;

	companion object {
		/**
		 * Converts a string to a [LvaType]
		 * @param type The string to convert
		 * @return The converted [LvaType]
		 */
		fun fromString(type: String): LvaType {
			return when (type) {
				"VL" -> VL
				"VO" -> VO
				"UE" -> UE
				"KV" -> KV
				"PR" -> PR
				"KS" -> KS
				else -> Unknown
			}
		}
	}
}