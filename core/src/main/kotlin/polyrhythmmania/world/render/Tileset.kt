package polyrhythmmania.world.render

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion


abstract class Tileset(val texture: Texture) {
    
    abstract val cube: TextureRegion
    abstract val cubeWithLine: TextureRegion
    abstract val cubeWithBlackBorder: TextureRegion
    abstract val platform: TextureRegion
    abstract val platformWithLine: TextureRegion
    
    abstract val padARetracted: TextureRegion
    abstract val padAExtended: TextureRegion
    abstract val padAPartial: TextureRegion
    abstract val padDRetracted: TextureRegion
    abstract val padDExtended: TextureRegion
    abstract val padDPartial: TextureRegion
    
    abstract val rodGroundFrames: Int
    abstract val rodAerialFrames: Int
    abstract val rodGroundAnimations: List<TextureRegion>
    abstract val rodAerialAnimations: List<TextureRegion>
    
    abstract val rodExplodeFrames: Int
    abstract val rodExplodeAnimations: List<TextureRegion>
    
    abstract val indicatorA: TextureRegion
    abstract val indicatorD: TextureRegion

    /**
     * 5 entries in order of A, +, BO, TA, N
     */
    abstract val buttonSigns: List<TextureRegion>
}

open class GBATileset(texture: Texture) : Tileset(texture) {
    override val cube: TextureRegion = TextureRegion(texture, 1, 1, 32, 32)
    override val cubeWithLine: TextureRegion = TextureRegion(texture, 34, 1, 32, 32)
    override val cubeWithBlackBorder: TextureRegion = TextureRegion(texture, 170, 93, 16, 8)
    override val platform: TextureRegion = TextureRegion(texture, 67, 1, 32, 32)
    override val platformWithLine: TextureRegion = TextureRegion(texture, 100, 1, 32, 32)
    
    override val padARetracted: TextureRegion = TextureRegion(texture, 1, 35, 32, 40)
    override val padAExtended: TextureRegion = TextureRegion(texture, 34, 35, 32, 40)
    override val padAPartial: TextureRegion = TextureRegion(texture, 67, 35, 32, 40)
    override val padDRetracted: TextureRegion = TextureRegion(texture, 1, 77, 32, 40)
    override val padDExtended: TextureRegion = TextureRegion(texture, 34, 77, 32, 40)
    override val padDPartial: TextureRegion = TextureRegion(texture, 67, 77, 32, 40)
    
    override val rodGroundFrames: Int = 6
    override val rodAerialFrames: Int = 6
    override val rodGroundAnimations: List<TextureRegion> by lazy {
        (0 until rodGroundFrames).map { i ->
            TextureRegion(texture, 205, 1 + 17 * i, 24, 16)
        }
    }
    override val rodAerialAnimations: List<TextureRegion> by lazy {
        (0 until rodAerialFrames).map { i ->
            TextureRegion(texture, 231, 1 + 17 * i, 24, 16)
        }
    }
    
    override val rodExplodeFrames: Int = 4
    override val rodExplodeAnimations: List<TextureRegion> = listOf(
            TextureRegion(texture, 1, 119, 40, 24),
            TextureRegion(texture, 7, 145, 32, 24),
            TextureRegion(texture, 11, 171, 24, 16),
            TextureRegion(texture, 14, 189, 16, 16),
    )
    
    override val indicatorA: TextureRegion = TextureRegion(texture, 169, 39, 16, 16)
    override val indicatorD: TextureRegion = TextureRegion(texture, 186, 39, 16, 16)
    
    override val buttonSigns: List<TextureRegion> = listOf(
//            TextureRegion(texture, 49, 120, 16, 16), // A with pixel in top right
            TextureRegion(texture, 83, 137, 16, 16), // A without pixel in top right
            
            TextureRegion(texture, 49, 137, 16, 16), // +
            
            TextureRegion(texture, 66, 120, 16, 16), // BO
            TextureRegion(texture, 83, 120, 16, 16), // TA
            TextureRegion(texture, 100, 120, 16, 16), // N
    )
}

class GBA2Tileset(texture: Texture) : GBATileset(texture) {
    override val cube: TextureRegion = TextureRegion(texture, 133, 1, 32, 32)
    override val cubeWithLine: TextureRegion = TextureRegion(texture, 166, 1, 32, 32)

    override val padAExtended: TextureRegion = TextureRegion(texture, 100, 35, 32, 40)
    override val padAPartial: TextureRegion = TextureRegion(texture, 133, 35, 32, 40)
    
    override val padDExtended: TextureRegion = TextureRegion(texture, 100, 77, 32, 40)
    override val padDPartial: TextureRegion = TextureRegion(texture, 133, 77, 32, 40)
    
    override val buttonSigns: List<TextureRegion> = listOf(
//            TextureRegion(texture, 49, 154, 16, 16), // A with pixel in top right
            TextureRegion(texture, 100, 137, 16, 16), // A without pixel in top right
            
            TextureRegion(texture, 66, 137, 16, 16), // +
            
            TextureRegion(texture, 66, 154, 16, 16), // BO
            TextureRegion(texture, 83, 154, 16, 16), // TA
            TextureRegion(texture, 100, 154, 16, 16), // N
    )
}
