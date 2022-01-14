package polyrhythmmania.screen.play

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.ui.UIElement
import paintbox.util.MathHelper


class ArrowNode(val tex: TextureRegion) : UIElement() {
    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val renderBounds = this.contentZone
        val x = renderBounds.x.get() + originX
        val y = originY - renderBounds.y.get()
        val w = renderBounds.width.get()
        val h = renderBounds.height.get()
        val offsetXMax = (w * 0.35f)
        val offsetX = (MathHelper.getSawtoothWave(1f) * 4f).coerceIn(0f, 1f) * offsetXMax
        batch.draw(tex, x + offsetX - offsetXMax, y - h,
                0.5f * w, 0.5f * h,
                w, h, 1f, 1f, 0f)
    }
}