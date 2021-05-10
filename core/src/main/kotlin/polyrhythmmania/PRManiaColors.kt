package polyrhythmmania

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Colors


object PRManiaColors {
    
    val TEMPO: Color = Color(0.4f, 0.4f, 0.9f, 1f)
    
    init {
        fun put(key: String, color: Color) = Colors.put(key, color)
        
        put("prmania_negative", Color.valueOf("#FF4C4C"))
        put("prmania_note", Color.CYAN.cpy())
        put("prmania_keystroke", Color.LIGHT_GRAY.cpy())
        put("prmania_playbackstart", Color.valueOf("#32FF32"))
        put("prmania_tempo", TEMPO)
    }
    
}