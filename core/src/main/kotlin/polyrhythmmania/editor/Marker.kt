package polyrhythmmania.editor

import com.badlogic.gdx.graphics.Color
import io.github.chrislo27.paintbox.binding.FloatVar
import polyrhythmmania.PRManiaColors


data class Marker(val type: MarkerType, val beat: FloatVar = FloatVar(0f)) {
}

enum class MarkerType(val color: Color, val localizationKey: String) {
    PLAYBACK_START(PRManiaColors.PLAYBACK_START, "editor.marker.playbackStart"),
    MUSIC_FIRST_BEAT(PRManiaColors.MUSIC_FIRST_BEAT, "editor.marker.musicFirstBeat"),
    
    ;
    
    companion object {
        val VALUES: List<MarkerType> = values().toList()
    }
}