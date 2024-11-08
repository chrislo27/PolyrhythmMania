package polyrhythmmania.world.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import paintbox.util.ColorStack
import paintbox.util.gdxutils.drawRect
import polyrhythmmania.world.World
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.Tileset


/**
 * Debug entity that shows the normal zoom frame. Zoom can be changed by editing [zoom].
 */
class EntityCameraFrame(world: World, color: Color, val lockToCamera: Boolean = false) : SimpleRenderedEntity(world) {

    private val color: Color = color.cpy()
    
    var zoom: Float = 1f
    
    override val renderHeight: Float = 5f * zoom
    override val renderWidth: Float get() = renderHeight * (16f / 9) * zoom

    override val renderSortOffsetZ: Float get() = if (lockToCamera) 9999f else 9000f

    override fun renderSimple(renderer: WorldRenderer, batch: SpriteBatch, tileset: Tileset, vec: Vector3) {
        val camera = renderer.camera
        val camW = camera.viewportWidth * zoom
        val camH = camera.viewportHeight * zoom

        val renderX = if (lockToCamera) camera.position.x - camW / 2 else vec.x
        val renderY = if (lockToCamera) camera.position.y - camH / 2 else vec.y
        
        val tmpColor = ColorStack.getAndPush()
        
        batch.color = tmpColor.set(this.color).apply { a *= 0.25f }
        batch.drawRect(renderX, renderY, camW, camH, 5 / 32f)
        batch.drawRect(renderX, renderY, camW, camH, 2 / 32f)
        batch.color = tmpColor.set(this.color)
        batch.drawRect(renderX, renderY, camW, camH, 1 / 32f)
        
        batch.setColor(1f, 1f, 1f, 1f)
        ColorStack.pop()
    }
}