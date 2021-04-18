package io.github.chrislo27.paintbox.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import io.github.chrislo27.paintbox.binding.Var
import kotlin.math.max
import kotlin.math.min


open class ImageNode(tex: TextureRegion? = null,
                renderingMode: ImageRenderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO)
    : UIElement() {
    
    val textureRegion: Var<TextureRegion?> = Var(tex)
    val tint: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val renderingMode: Var<ImageRenderingMode> = Var(renderingMode)
    val rotation: Var<Float> = Var(0f)
    val rotationPointX: Var<Float> = Var(0.5f)
    val rotationPointY: Var<Float> = Var(0.5f)

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val tex = textureRegion.getOrCompute()
        if (tex != null) {
            val old = batch.packedColor
            
            val tmpColor = ColorStack.getAndPush()
            tmpColor.set(tint.getOrCompute())
            val opacity = apparentOpacity.getOrCompute()
            tmpColor.a *= opacity
            
            batch.color = tmpColor

            val renderBounds = this.contentZone
            val x = renderBounds.x.getOrCompute() + originX
            val y = originY - renderBounds.y.getOrCompute()
            val w = renderBounds.width.getOrCompute()
            val h = renderBounds.height.getOrCompute()
            
            when (val renderingMode = this.renderingMode.getOrCompute()) {
                ImageRenderingMode.FULL -> {
                    batch.draw(tex, x, y - h,
                               rotationPointX.getOrCompute() * w, rotationPointY.getOrCompute() * h,
                               w, h, 1f, 1f, rotation.getOrCompute())
                }
                ImageRenderingMode.MAINTAIN_ASPECT_RATIO, ImageRenderingMode.OVERSIZE -> {
                    val aspectWidth = w / tex.regionWidth
                    val aspectHeight = h / tex.regionHeight
                    val aspectRatio = if (renderingMode == ImageRenderingMode.MAINTAIN_ASPECT_RATIO)
                        min(aspectWidth, aspectHeight) else max(aspectWidth, aspectHeight)
                    
                    val rw: Float = tex.regionWidth * aspectRatio
                    val rh: Float = tex.regionHeight * aspectRatio
                    val rx: Float = w / 2 - (rw / 2)
                    val ry: Float = h / 2 - (rh / 2)

                    batch.draw(tex, x + rx, y + ry - rh,
                               rotationPointX.getOrCompute() * rw, rotationPointY.getOrCompute() * rh,
                               rw, rh,
                               1f, 1f,
                               rotation.getOrCompute())
                }
            }
            
            ColorStack.pop()

            batch.packedColor = old
        }
    }
    
}

enum class ImageRenderingMode {

    /**
     * Draws the texture region at the full bounds of this element.
     */
    FULL,

    /**
     * Maintains the texture region's original aspect ratio, but doesn't oversize past the UI element bounds.
     */
    MAINTAIN_ASPECT_RATIO,

    /**
     * Maintains the texture region's original aspect ratio, but can oversize.
     */
    OVERSIZE,

}