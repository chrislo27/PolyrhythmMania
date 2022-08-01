package polyrhythmmania.library.menu

import polyrhythmmania.library.LevelEntry
import java.util.*


sealed class Filter(open val enabled: Boolean, open val filterable: Filterable) {

    abstract fun filter(levelEntry: LevelEntry.Modern): Boolean
    
    abstract fun copyBase(enabled: Boolean): Filter

}

data class FilterOnStringList(override val enabled: Boolean, override val filterable: Filterable,
                              val filterOn: String, val list: List<String>)
    : Filter(enabled, filterable) {
    
    private val filterLower: String = filterOn.lowercase(Locale.ROOT)

    override fun filter(levelEntry: LevelEntry.Modern): Boolean {
        val compareTo: String = (when (filterable) {
            Filterable.LEVEL_CREATOR -> levelEntry.levelMetadata.levelCreator
            Filterable.SONG_NAME -> levelEntry.levelMetadata.songName
            Filterable.SONG_ARTIST -> levelEntry.levelMetadata.songArtist
            Filterable.ALBUM_NAME -> levelEntry.levelMetadata.albumName
            Filterable.GENRE -> levelEntry.levelMetadata.genre
            else -> return false
        }).lowercase(Locale.ROOT)
        if (filterOn == "") return true
        return compareTo == filterLower
    }

    override fun copyBase(enabled: Boolean): FilterOnStringList {
        return this.copy(enabled = enabled)
    }
}

data class FilterInteger(override val enabled: Boolean, override val filterable: Filterable,
                         val op: Op, val right: Int, val ignoreZero: Boolean)
    : Filter(enabled, filterable) {

    enum class Op(val symbol: String) {
        EQ("="), GT(">"), LT("<"), GEQ(">="), LEQ("<=");
        companion object {
            val VALUES: List<Op> = values().toList()
        }
    }

    override fun filter(levelEntry: LevelEntry.Modern): Boolean {
        val compareTo: Int = when (filterable) {
            Filterable.ALBUM_YEAR -> levelEntry.levelMetadata.albumYear
            Filterable.DIFFICULTY -> levelEntry.levelMetadata.difficulty
            else -> return false
        }
        if (ignoreZero && compareTo == 0) return false
        return when (op) {
            Op.EQ -> compareTo == right
            Op.GT -> compareTo > right
            Op.LT -> compareTo < right
            Op.GEQ -> compareTo >= right
            Op.LEQ -> compareTo <= right
        }
    }

    override fun copyBase(enabled: Boolean): FilterInteger {
        return this.copy(enabled = enabled)
    }
}

data class FilterBoolean(override val enabled: Boolean, override val filterable: Filterable,
                         val targetValue: Boolean)
    : Filter(enabled, filterable) {

    override fun filter(levelEntry: LevelEntry.Modern): Boolean {
        val compareTo: Boolean = when (filterable) {
            Filterable.FLASHING_LIGHTS_WARNING -> levelEntry.levelMetadata.flashingLightsWarning
            else -> return false
        }
        return compareTo == targetValue
    }

    override fun copyBase(enabled: Boolean): FilterBoolean {
        return this.copy(enabled = enabled)
    }
}

