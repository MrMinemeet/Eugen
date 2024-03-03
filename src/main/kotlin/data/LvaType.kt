package data

enum class LvaType {
	VL,
	VO,
	UE,
	VU,
	KV,
	PR,
	PS,
	KS,
	SE,
	Unknown;

	companion object {
		/**
		 * Converts a string to a [LvaType]
		 * @param type The string to convert
		 * @return The converted [LvaType]
		 */
		fun fromString(type: String): LvaType {
			return when (type.uppercase()) {
				"VL" -> VL
				"VO" -> VO
				"UE" -> UE
				"VU" -> VU
				"KV" -> KV
				"PR" -> PR
				"PS" -> PS
				"KS" -> KS
				"SE" -> SE
				else -> Unknown
			}
		}
	}
}