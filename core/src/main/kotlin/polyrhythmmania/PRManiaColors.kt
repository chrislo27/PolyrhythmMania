package polyrhythmmania

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Colors


object PRManiaColors {
    
    init {
        fun put(key: String, color: Color) = Colors.put(key, color)
        
        put("prmania_negative", Color.valueOf("#FF4C4C"))
        put("prmania_playbackstart", Color.valueOf("#32FF32"))
    }
    
}