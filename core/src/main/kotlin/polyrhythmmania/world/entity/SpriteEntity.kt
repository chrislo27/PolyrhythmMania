package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import paintbox.util.ColorStack
import polyrhythmmania.world.World
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TintedRegion

abstract class SpriteEntity(world: World) : SimpleRenderedEntity(world) {
    
    open val numLayers: Int = 1
    var tint: Color? = null
    var tintIsMultiplied: Boolean = true


    protected open val pxOffsetX: Float = 0f
    protected open val pxOffsetY: Float = 0f
    
    abstract fun getTintedRegion(tileset: Tileset, index: Int): TintedRegion?

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        val tmpColor = ColorStack.getAndPush()
        val tint = this.tint
        for (i in 0 until numLayers) {
            val tr = getTintedRegion(tileset, i)
            if (tr != null) {
                if (tintIsMultiplied) {
                    tmpColor.set(tr.color.getOrCompute())
                    if (tint != null) {
                        tmpColor.r *= tint.r
                        tmpColor.g *= tint.g
                        tmpColor.b *= tint.b
                        tmpColor.a *= tint.a
                        // Intentionally don't clamp values.
                    }
                } else {
                    if (tint != null) tmpColor.set(tint)
                }
                drawTintedRegion(batch, vec, tileset, tr, pxOffsetX, pxOffsetY, renderWidth, renderHeight, tmpColor)
            }
        }
        ColorStack.pop()
    }
}
