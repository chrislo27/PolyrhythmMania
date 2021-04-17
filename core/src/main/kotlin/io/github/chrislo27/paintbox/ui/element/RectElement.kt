package io.github.chrislo27.paintbox.ui.element

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.chrislo27.paintbox.ui.ColorStack
import io.github.chrislo27.paintbox.ui.UIElement
import io.github.chrislo27.paintbox.util.Var
import io.github.chrislo27.paintbox.util.gdxutils.drawRect
import io.github.chrislo27.paintbox.util.gdxutils.fillRect


class RectElement(initColor: Color) : UIElement() {
    
    val color: Var<Color> = Var(Color(1f, 1f, 1f, 1f).set(initColor))
    val borderColor: Var<Color> = Var { color.use() }
    val borderWidth: Var<Float> = Var(0f)
    
    constructor() : this(Color.WHITE)
    
    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val x = bounds.x.getOrCompute() + originX
        val y = originY - bounds.y.getOrCompute()
        val w = bounds.width.getOrCompute()
        val h = bounds.height.getOrCompute()
        val lastPackedColor = batch.packedColor
        
        val opacity: Float = this.apparentOpacity.getOrCompute()
        val tmpColor: Color = ColorStack.getAndPush()
        tmpColor.set(color.getOrCompute())
        tmpColor.a *= opacity
        batch.color = tmpColor
        batch.fillRect(x, y - h, w, h)
        
        tmpColor.set(borderColor.getOrCompute())
        tmpColor.a *= opacity
        if (tmpColor.a > 0f) {
            val borderWidth = this.borderWidth.getOrCompute()
            if (borderWidth > 0f) {
                batch.color = tmpColor
                batch.drawRect(x, y - h, w, h, borderWidth)
            }
        }
        
        ColorStack.pop()
        batch.packedColor = lastPackedColor
    }
}