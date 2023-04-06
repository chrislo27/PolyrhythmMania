package polyrhythmmania.storymode.gamemode.boss.scripting

import com.badlogic.gdx.graphics.Color

data class LightStrength(
    val ambient: Float, 
    val selected: Float,
    val ambientLightColorOverride: Color? = null
) {
    
    companion object {

        val NORMAL: LightStrength = LightStrength(1.0f, 0.0f)
        val DARK1: LightStrength = LightStrength(0.15f, 1.0f)
        val DARK2: LightStrength = LightStrength(0.05f, 0.5f)
        val DARK3: LightStrength = LightStrength(0.0f, 0.25f)
        val DARK_BOSS_INTRO: LightStrength = LightStrength(0.0f, 0.75f, Color(0f, 0f, 0f, 1f))
    }
}