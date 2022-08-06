package polyrhythmmania.world.spotlights

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import polyrhythmmania.world.World


open class LightColor(val resetColor: Color, val defaultStrength: Float) {
    
    /*
    Colour and strength outputs:
    
    For a spotlight:
        - RGB is RGB, closer to white = more visible
        - Higher A * Strength = more visible through the darkness
        - Total formula:
            - (R, G, B, A * Strength)
    
    For ambient light:
        - RGB is RGB, closer to white = rest of scene is more visible. Closer to black = darker overall
        - Alpha does not do anything in actual rendering, so it has to be converted to RGB:
            - A=1 means full darkness, A=0 means no darkness.
            - If A=0, then RGB is full white. Therefore the interpolation is (from RGB to white, (1.0 - A)%)
        - Full strength = full lighting, therefore RGB becomes full white. 0 strength = full RGB.
            - The interpolation is (from RGB to white, (strength)%) 
        - Total formula:
            - lerp (from RGB to white, (1.0 - A)%)
            - lerp (from RGB to white, (strength)%) 
     */
    
    val color: Color = Color(1f, 1f, 1f, 1f).set(resetColor)
    var strength: Float = defaultStrength


    fun reset() {
        color.set(resetColor)
        strength = defaultStrength
    }
    
    fun isInDefaultState(): Boolean {
        return color == resetColor && strength == defaultStrength
    }
    
    /**
     * For [Spotlight]: Modifies [tmpColor] by setting it to [color] and multiplying the alpha by [strength].
     */
    fun computeFinalForSpotlight(tmpColor: Color): Color {
        return tmpColor.set(color).also { c ->
            c.a *= strength
        }
    }
    
    /**
     * For ambient light: Modifies [tmpColor] by applying the formula above.
     */
    fun computeFinalForAmbientLight(tmpColor: Color): Color {
        return tmpColor.set(color).lerp(Color.WHITE, 1f - color.a).lerp(Color.WHITE, strength).also { c ->
            c.a = 1f
        }
    }
    
}

class Spotlight(val spotlightsParent: Spotlights) {
    
    val lightColor: LightColor = LightColor(Spotlights.SPOTLIGHT_RESET_COLOR, 0f)
    val position: Vector3 = Vector3() // Indicates base of light

    fun reset() {
        lightColor.reset()
    }
}

class Spotlights(val world: World) {
    
    companion object {
        const val NUM_ON_ROW: Int = 10
        val AMBIENT_LIGHT_RESET_COLOR: Color = Color(0f, 0f, 0f, 240 / 255f)
        val SPOTLIGHT_RESET_COLOR: Color = Color(1f, 1f, 1f, 1f)
    }
    
    val ambientLight: LightColor = LightColor(AMBIENT_LIGHT_RESET_COLOR, 1f)
    
    val spotlightsRowA: List<Spotlight>
    val spotlightsRowDpad: List<Spotlight>
    val allSpotlights: List<Spotlight>

    init {
        fun buildRow(z: Float): List<Spotlight> {
            return (0 until NUM_ON_ROW).map { i ->
                Spotlight(this).apply {
                    this.position.set(5f + 0.5f + i, 2f, z + 0.5f)
                }
            }
        }
        this.spotlightsRowA = buildRow(0f)
        this.spotlightsRowDpad = buildRow(-3f)
        this.allSpotlights = (spotlightsRowA + spotlightsRowDpad)
        
        onWorldReset()
    }
    
    
    fun onWorldReset() {
        ambientLight.reset()
        allSpotlights.forEach(Spotlight::reset)
    }
    
    fun isAmbientLightingFull(): Boolean = ambientLight.strength >= 1f || ambientLight.color.a <= 0f
    
}
