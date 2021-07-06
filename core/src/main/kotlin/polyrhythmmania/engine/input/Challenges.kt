package polyrhythmmania.engine.input

import com.badlogic.gdx.graphics.Color
import polyrhythmmania.engine.Engine


data class Challenges(val tempoUp: Int, val goingForPerfect: Boolean) {
    companion object {
        val NO_CHANGES: Challenges = Challenges(100, false)
        
        val TEMPO_DOWN_COLOR: Color = Color.valueOf("3E5BEF")
        val TEMPO_UP_COLOR: Color = Color.valueOf("ED3D3D")
    }
    
    fun applyToEngine(engine: Engine) {
        engine.playbackSpeed = tempoUp / 100f
        engine.inputter.challenge.goingForPerfect = goingForPerfect
    }
}
