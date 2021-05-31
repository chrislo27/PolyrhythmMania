package io.github.chrislo27.paintbox.util


data class Version(val major: Int, val minor: Int, val patch: Int, val suffix: String = "")
    : Comparable<Version> {
    
    companion object {
        val REGEX: Regex = "v?(\\d+).(\\d+).(\\d+)(?:-(.+))?".toRegex()
        
        fun parse(thing: String): Version? {
            val match: MatchResult = REGEX.matchEntire(thing) ?: return null

            return try {
                Version(Integer.parseInt(match.groupValues[1]), Integer.parseInt(match.groupValues[2]),
                        Integer.parseInt(match.groupValues[3]), match.groupValues.getOrNull(4) ?: "")
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private val stringRepresentation: String = "$major.$minor.$patch${if (suffix.isNotEmpty()) "-$suffix" else ""}"
    private val vstringRepresentation: String = "v$stringRepresentation"

    override fun compareTo(other: Version): Int {
        return if (this.major < other.major) -1 else if (this.major > other.major) 1
        else {
            if (this.minor < other.minor) -1 else if (this.minor > other.minor) 1
            else {
                if (this.patch < other.patch) -1 else if (this.patch > other.patch) 1
                else 0
            }
        }
    }

    override fun toString(): String {
        return vstringRepresentation
    }
    
    fun toNonVString(): String = stringRepresentation
}
