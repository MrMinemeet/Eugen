package org.example.data

enum class LvaType {
	VL,
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
				"VL", "VO" -> VL
				"UE" -> UE
				"KV" -> KV
				"PR" -> PR
				"KS" -> KS
				else -> Unknown
			}
		}
	}
}