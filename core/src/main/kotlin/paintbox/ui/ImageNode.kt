package paintbox.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import paintbox.binding.FloatVar
import paintbox.binding.Var
import paintbox.util.ColorStack
import paintbox.util.gdxutils.drawUV
import kotlin.math.max
import kotlin.math.min



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

open class ImageNode(tex: TextureRegion? = null,
                     renderingMode: ImageRenderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO)
    : UIElement() {

    val textureRegion: Var<TextureRegion?> = Var(tex)
    val tint: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val renderingMode: Var<ImageRenderingMode> = Var(renderingMode)
    val rotation: FloatVar = FloatVar(0f)
    val rotationPointX: FloatVar = FloatVar(0.5f)
    val rotationPointY: FloatVar = FloatVar(0.5f)
    val renderAlign: Var<Int> = Var(Align.center)

    constructor(binding: Var.Context.() -> TextureRegion?,
                renderingMode: ImageRenderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO)
            : this(null, renderingMode) {
        textureRegion.bind(binding)
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val tex = textureRegion.getOrCompute()
        if (tex != null) {
            val old = batch.packedColor

            val tmpColor = ColorStack.getAndPush()
            tmpColor.set(tint.getOrCompute())
            val opacity = apparentOpacity.get()
            tmpColor.a *= opacity

            batch.color = tmpColor

            val renderBounds = this.contentZone
            val x = renderBounds.x.get() + originX
            val y = originY - renderBounds.y.get()
            val w = renderBounds.width.get()
            val h = renderBounds.height.get()

            val rotPointX = rotationPointX.get()
            val rotPointY = rotationPointY.get()
            val rot = rotation.get()
            when (val renderingMode = this.renderingMode.getOrCompute()) {
                ImageRenderingMode.FULL -> {
                    batch.draw(tex, x, y - h,
                            rotPointX * w, rotPointY * h,
                            w, h, 1f, 1f, rot)
                }
                ImageRenderingMode.MAINTAIN_ASPECT_RATIO, ImageRenderingMode.OVERSIZE -> {
                    val aspectWidth = w / tex.regionWidth
                    val aspectHeight = h / tex.regionHeight
                    val aspectRatio = if (renderingMode == ImageRenderingMode.MAINTAIN_ASPECT_RATIO)
                        min(aspectWidth, aspectHeight) else max(aspectWidth, aspectHeight)

                    val rw: Float = tex.regionWidth * aspectRatio
                    val rh: Float = tex.regionHeight * aspectRatio
                    val align = this.renderAlign.getOrCompute()
                    val xOffset: Float = when {
                        Align.isLeft(align) -> 0f
                        Align.isRight(align) -> w - rw
                        else -> w / 2 - (rw / 2)
                    }
                    val yOffset: Float = when {
                        Align.isTop(align) -> h - rh
                        Align.isBottom(align) -> 0f
                        else -> h / 2 - (rh / 2)
                    }
                    val rx: Float = xOffset
                    val ry: Float = yOffset


                    batch.draw(tex, x + rx, y + ry - h,
                            rotPointX * rw, rotPointY * rh,
                            rw, rh,
                            1f, 1f,
                            rot)
                }
            }

            ColorStack.pop()

            batch.packedColor = old
        }
    }
}

open class ImageIcon(tex: TextureRegion? = null,
                     renderingMode: ImageRenderingMode = ImageRenderingMode.MAINTAIN_ASPECT_RATIO)
    : ImageNode(tex, renderingMode), HasTooltip {
    
    override val tooltipElement: Var<UIElement?> = Var(null)
}

/**
 * A full-render image node, but with uv settings to control the render window of the top-left/bottom-right corners encompassed.
 */
open class ImageWindowNode(tex: TextureRegion? = null)
    : UIElement() {

    val textureRegion: Var<TextureRegion?> = Var(tex)
    val tint: Var<Color> = Var(Color(1f, 1f, 1f, 1f))
    val windowU: FloatVar = FloatVar(0f) 
    val windowV: FloatVar = FloatVar(0f) 
    val windowU2: FloatVar = FloatVar(1f) 
    val windowV2: FloatVar = FloatVar(1f) 

    constructor(binding: Var.Context.() -> TextureRegion?)
            : this(null) {
        textureRegion.bind(binding)
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val tex = textureRegion.getOrCompute()
        if (tex != null) {
            val old = batch.packedColor

            val tmpColor = ColorStack.getAndPush()
            tmpColor.set(tint.getOrCompute())
            val opacity = apparentOpacity.get()
            tmpColor.a *= opacity

            batch.color = tmpColor

            val renderBounds = this.contentZone
            val x = renderBounds.x.get() + originX
            val y = originY - renderBounds.y.get()
            val w = renderBounds.width.get()
            val h = renderBounds.height.get()
            
            val u = this.windowU.get()
            val v = this.windowV.get()
            val u2 = this.windowU2.get()
            val v2 = this.windowV2.get()

            batch.drawUV(tex.texture,
                    x + w * u,
                    y - h + h * v,
                    w * (u2 - u),
                    h * (v2 - v),
                    MathUtils.lerp(tex.u, tex.u2, u),
                    MathUtils.lerp(tex.v, tex.v2, v),
                    MathUtils.lerp(tex.u, tex.u2, u2),
                    MathUtils.lerp(tex.v, tex.v2, v2)
            )

            ColorStack.pop()

            batch.packedColor = old
        }
    }
}