package polyrhythmmania.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3


class Spotlight(val spotlightsParent: Spotlights) {
    
    var enabled: Boolean = false
    var strength: Float = 1f
    val light: Color = Color(1f, 1f, 1f, 1f)
    
    val position: Vector3 = Vector3() // Indicates base of light

    fun reset() {
        light.set(1f, 1f, 1f, 1f)
        strength = 1f
        enabled = false
    }
}

class Spotlights(val world: World) {
    
    companion object {
        const val NUM_ON_ROW: Int = 10
    }
    
    val ambientLight: Color = Color(1f, 1f, 1f, 1f)
    
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
    }
    
    
    fun onWorldReset() {
        ambientLight.set(1f, 1f, 1f, 1f)
        allSpotlights.forEach(Spotlight::reset)
    }

    /**
     * Returns [tmpColor], set to [ambientLight] with its alpha "premultiplied". 
     * This is not true alpha premultiplication because it linearly interpolates to white based on the alpha.
     */
    fun getAmbientLightPremultipliedAlpha(tmpColor: Color): Color {
        return tmpColor.set(1f, 1f, 1f, 1f).lerp(ambientLight.r, ambientLight.g, ambientLight.b, 1f, 1f - ambientLight.a)
    }
    
}
