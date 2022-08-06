package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import paintbox.util.Vector3Stack
import paintbox.util.gdxutils.drawUV
import polyrhythmmania.world.World
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset
import polyrhythmmania.world.tileset.TintedRegion
import polyrhythmmania.world.tileset.TintedSubregion

open class SimpleRenderedEntity(world: World) : Entity(world) {
    
    protected open fun getRenderVec(): Vector3 {
        return this.position
    }
    
    override fun render(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset) {
        val tmpVec = Vector3Stack.getAndPush()
        val convertedVec = WorldRenderer.convertWorldToScreen(tmpVec.set(getRenderVec()))
        val packedColor = batch.packedColor
        renderSimple(renderer, batch, tileset, convertedVec)
        Vector3Stack.pop()
        batch.packedColor = packedColor
    }
    
    protected open fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
    }
    
    protected fun drawTintedRegion(batch: SpriteBatch, vec: Vector3, tileset: Tileset, tintedRegion: TintedRegion,
                                   offsetX: Float, offsetY: Float, renderWidth: Float, renderHeight: Float,
                                   tintColor: Color? = null) {
        if (renderWidth == 0f || renderHeight == 0f) return
        
        val tilesetRegion = tileset.getTilesetRegionForTinted(tintedRegion)
        var offX = offsetX
        var offY = offsetY
        var drawWidth = renderWidth
        var drawHeight = renderHeight
        val spacingObj = tilesetRegion.spacing
        
        if (spacingObj.spacing > 0 && spacingObj.normalWidth > 0 && spacingObj.normalHeight > 0) {
            val spacing = spacingObj.spacing
            val totalNormalWidth = spacingObj.normalWidth + spacing * 2
            val totalNormalHeight = spacingObj.normalHeight + spacing * 2
            val totalNormalWidthRatio = totalNormalWidth.toFloat() / spacingObj.normalWidth
            val totalNormalHeightRatio = totalNormalHeight.toFloat() / spacingObj.normalHeight
            
            offX -= spacing.toFloat() / totalNormalWidth
            offY -= spacing.toFloat() / totalNormalHeight
            
            drawWidth *= totalNormalWidthRatio
            drawHeight *= totalNormalHeightRatio
        }
        
        batch.color = tintColor ?: tintedRegion.color.getOrCompute()
        // Compute special UV regions for TintedSubregion
        val texture = tilesetRegion.texture
        var u = tilesetRegion.u
        var v = tilesetRegion.v
        var u2 = tilesetRegion.u2
        var v2 = tilesetRegion.v2
        if (tintedRegion is TintedSubregion) {
            val uSpan = u2 - u
            val vSpan = v2 - v
            u = MathUtils.lerp(u, u2, tintedRegion.u)
            v = MathUtils.lerp(v, v2, tintedRegion.v)
            u2 = u + uSpan * tintedRegion.u2
            v2 = v + vSpan * tintedRegion.v2
        }
        batch.drawUV(texture, vec.x + offX, vec.y + offY, drawWidth, drawHeight, u, v, u2, v2)
        batch.setColor(1f, 1f, 1f, 1f)
    }
    
    protected fun drawTintedRegion(batch: SpriteBatch, vec: Vector3, tileset: Tileset, tintedRegion: TintedRegion,
                                   tintColor: Color? = null) {
        drawTintedRegion(batch, vec, tileset, tintedRegion, 0f, 0f, renderWidth, renderHeight, tintColor)
    }
}
