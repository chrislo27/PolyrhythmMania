package io.github.chrislo27.paintbox.util


data class Version(val major: Int, val minor: Int, val patch: Int, val suffix: String = "")
    : Comparable<Version> {
    
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
