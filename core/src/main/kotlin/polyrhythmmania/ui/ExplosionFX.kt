package polyrhythmmania.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.binding.FloatVar
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.UIElement
import polyrhythmmania.world.entity.EntityExplosion
import kotlin.math.min


class ExplosionFX(val style: TilesetStyle, val onFinish: EndBehaviour) : UIElement() {
    
    enum class TilesetStyle(val packedSheetID: String) {
        GBA("tileset_gba"), HD("tileset_hd"), ARCADE("tileset_arcade")
    }
    
    enum class EndBehaviour {
        DELETE, DO_NOTHING, LOOP
    }
    
    val speed: FloatVar = FloatVar(1f)
    val scale: FloatVar = FloatVar(1f)
    var animationDuration: Float = 1f
    
    private var deleted: Boolean = false

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        if (animationDuration <= 0f) return
        
        val renderBounds = this.contentZone
        val x = renderBounds.x.get() + originX
        val y = originY - renderBounds.y.get()
        val w = renderBounds.width.get()
        val h = renderBounds.height.get()
        
        val percentage = (1f - (animationDuration)).coerceIn(0f, 1f)
        val index = (percentage * EntityExplosion.STATES.size).toInt()
        val state = EntityExplosion.STATES.getOrNull(index)
        
        if (state != null) {
            val packedSheet = AssetRegistry.get<PackedSheet>(style.packedSheetID)
            val biggestTexReg = packedSheet.getIndexedRegions("explosion").getValue(0)
            val texReg = packedSheet.getIndexedRegions("explosion").getValue(state.index)
            
            val aspectRatio = min(w / biggestTexReg.regionWidth, h / biggestTexReg.regionHeight)
            val scale = this.scale.get()
            val rw: Float = texReg.regionWidth * aspectRatio * scale
            val rh: Float = texReg.regionHeight * aspectRatio * scale
            val rx: Float = w / 2 - (rw / 2)
            val ry: Float = h / 2 - (biggestTexReg.regionHeight * aspectRatio * scale / 2) //0f // Bottom aligned

            batch.draw(texReg, x + rx, y - h + ry, rw, rh)
        }

        animationDuration = (animationDuration - (Gdx.graphics.deltaTime * speed.get() / (EntityExplosion.EXPLOSION_DURATION))).coerceAtLeast(0f)
        if (animationDuration <= 0f) {
            when (onFinish) {
                EndBehaviour.DELETE -> {
                    if (!deleted) {
                        deleted = true
                        this.parent.getOrCompute()?.removeChild(this)
                    }
                }
                EndBehaviour.DO_NOTHING -> {}
                EndBehaviour.LOOP -> {
                    animationDuration = 1f
                }
            }
        }
    }
    
    fun reset() {
        animationDuration = 1f
    }
}