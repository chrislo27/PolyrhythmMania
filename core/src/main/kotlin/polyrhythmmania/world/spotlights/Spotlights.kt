package polyrhythmmania.world.spotlights

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import paintbox.util.ColorStack
import polyrhythmmania.world.World


class LightColor(private val resetColor: Color) {
    
    var enabled: Boolean = false
    val color: Color = Color(1f, 1f, 1f, 1f).set(resetColor)
    var strength: Float = 1f

    /**
     * Modifies [tmpColor] by setting it to [color] and multiplying the alpha by [strength].
     */
    fun multiplyStrength(tmpColor: Color): Color {
        return tmpColor.set(color).also { c ->
            c.a *= strength
        }
    }
    
    fun reset() {
        color.set(resetColor)
        strength = 1f
        enabled = false
    }
    
}

class Spotlight(val spotlightsParent: Spotlights) {
    
    val lightColor: LightColor = LightColor(Spotlights.SPOTLIGHT_RESET_COLOR)
    val position: Vector3 = Vector3() // Indicates base of light

    fun reset() {
        lightColor.reset()
    }
}

class Spotlights(val world: World) {
    
    companion object {
        const val NUM_ON_ROW: Int = 10
        val AMBIENT_LIGHT_RESET_COLOR: Color = Color.BLACK.cpy()
        val SPOTLIGHT_RESET_COLOR: Color = Color.WHITE.cpy()
    }
    
    val ambientLight: LightColor = LightColor(AMBIENT_LIGHT_RESET_COLOR)
    
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

    /**
     * Returns [tmpColor], set to [ambientLight] with its alpha "premultiplied". 
     * This is not true alpha premultiplication -- it linearly interpolates to white based on the alpha.
     */
    fun convertAmbientLightToRGB(tmpColor: Color): Color {
        val tmp2 = ambientLight.multiplyStrength(ColorStack.getAndPush())
        tmpColor.set(tmp2.r, tmp2.g, tmp2.b, 1f).lerp(1f, 1f, 1f, 1f, tmp2.a)
        ColorStack.pop()
        return tmpColor
    }
    
}
