package polyrhythmmania

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Colors


object PRManiaColors {
    
    val NEGATIVE: Color = Color.valueOf("#FF4C4C")
    val TEMPO: Color = Color(0.4f, 0.4f, 0.9f, 1f)
    val MUSIC_VOLUME: Color = Color(1f, 0.4f, 0f, 1f)
    val PLAYBACK_START: Color = Color.valueOf("#32FF32")
    val MUSIC_DELAY: Color = Color(1f, 0f, 0f, 1f)
    val MARKER_FIRST_BEAT: Color = Color(1f, 0f, 0f, 1f)
    val MARKER_LOOP_START: Color = Color(1f, 0.85f, 0f, 1f)
    val MARKER_LOOP_END: Color = Color(1f, 0.45f, 0f, 1f)
    
    init {
        fun put(key: String, color: Color) = Colors.put(key, color)
        
        put("prmania_negative", NEGATIVE.cpy())
        put("prmania_keystroke", Color.CYAN.cpy())
        put("prmania_statushint", Color.LIGHT_GRAY.cpy())
        put("prmania_tooltip_keystroke", Color.LIGHT_GRAY.cpy())
        put("prmania_playbackstart", PLAYBACK_START.cpy())
        put("prmania_musicdelay", MUSIC_DELAY.cpy())
        put("prmania_tempo", TEMPO.cpy())
        put("prmania_musicvol", MUSIC_VOLUME.cpy())
        put("prmania_marker_firstbeat", MARKER_FIRST_BEAT.cpy())
        put("prmania_marker_loopstart", MARKER_LOOP_START.cpy())
        put("prmania_marker_loopend", MARKER_LOOP_END.cpy())
    }
    
}