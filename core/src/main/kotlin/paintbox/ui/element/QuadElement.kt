package paintbox.ui.element

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.PaintboxGame
import paintbox.util.ColorStack
import paintbox.ui.UIElement
import paintbox.binding.Var
import paintbox.util.gdxutils.drawQuad


/**
 * A quad describes a OpenGL quad with configurable colours for each corner.
 */
open class QuadElement(initTopLeft: Color, initTopRight: Color, initBottomLeft: Color, initBottomRight: Color)
    : UIElement() {
    
    val topLeftColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f).set(initTopLeft))
    val topRightColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f).set(initTopRight))
    val bottomLeftColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f).set(initBottomLeft))
    val bottomRightColor: Var<Color> = Var(Color(1f, 1f, 1f, 1f).set(initBottomRight))

    /**
     * Texture to use when filling the quad. By default it is null, which means it will use [paintbox.PaintboxGame.fillTexture].
     */
    val texture: Var<Texture?> = Var(null)
    
    constructor(initColorAll: Color) : this(initColorAll, initColorAll, initColorAll, initColorAll)
    constructor() : this(Color.WHITE)
    
    constructor(bindingAll: Var.Context.() -> Color) : this() {
        topLeftColor.bind(bindingAll)
        topRightColor.bind(bindingAll)
        bottomLeftColor.bind(bindingAll)
        bottomRightColor.bind(bindingAll)
    }
    
    fun leftRightGradient(left: Color, right: Color) {
        topLeftColor.set(left.cpy())
        bottomLeftColor.set(left.cpy())
        topRightColor.set(right.cpy())
        bottomRightColor.set(right.cpy())
    }
    
    fun leftRightGradient(left: Var.Context.() -> Color, right: Var.Context.() -> Color) {
        topLeftColor.bind(left)
        bottomLeftColor.bind(left)
        topRightColor.bind(right)
        bottomRightColor.bind(right)
    }
    
    fun topBottomGradient(top: Color, bottom: Color) {
        topLeftColor.set(top.cpy())
        topRightColor.set(top.cpy())
        bottomLeftColor.set(bottom.cpy())
        bottomRightColor.set(bottom.cpy())
    }
    
    fun topBottomGradient(top: Var.Context.() -> Color, bottom: Var.Context.() -> Color) {
        topLeftColor.bind(top)
        topRightColor.bind(top)
        bottomLeftColor.bind(bottom)
        bottomRightColor.bind(bottom)
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val renderBounds = this.paddingZone
        val x = renderBounds.x.get() + originX
        val y = originY - renderBounds.y.get()
        val w = renderBounds.width.get()
        val h = renderBounds.height.get()
        val lastPackedColor = batch.packedColor

        val opacity: Float = this.apparentOpacity.get()
        val tmpColorTL: Color = ColorStack.getAndPush()
        tmpColorTL.set(topLeftColor.getOrCompute())
        tmpColorTL.a *= opacity
        val tmpColorTR: Color = ColorStack.getAndPush()
        tmpColorTR.set(topRightColor.getOrCompute())
        tmpColorTR.a *= opacity
        val tmpColorBL: Color = ColorStack.getAndPush()
        tmpColorBL.set(bottomLeftColor.getOrCompute())
        tmpColorBL.a *= opacity
        val tmpColorBR: Color = ColorStack.getAndPush()
        tmpColorBR.set(bottomRightColor.getOrCompute())
        tmpColorBR.a *= opacity
        
        batch.setColor(1f, 1f, 1f, 1f)
        batch.drawQuad(x, y - h, tmpColorBL, x + w, y - h, tmpColorBR, x + w, y, tmpColorTR, x, y, tmpColorTL, 
                texture.getOrCompute() ?: PaintboxGame.fillTexture)

        ColorStack.pop()
        ColorStack.pop()
        ColorStack.pop()
        ColorStack.pop()
        batch.packedColor = lastPackedColor
    }
}