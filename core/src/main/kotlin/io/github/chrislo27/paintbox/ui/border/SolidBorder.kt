package io.github.chrislo27.paintbox.ui.border

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.ui.ColorStack
import io.github.chrislo27.paintbox.ui.UIElement
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.util.gdxutils.fillRect


class SolidBorder(initColor: Color) : Border {
    
    val color: Var<Color> = Var(Color(1f, 1f, 1f, 1f).set(initColor))
    
    constructor() : this(Color.WHITE)
    
    override fun renderBorder(originX: Float, originY: Float, batch: SpriteBatch, element: UIElement) {
        val insets = element.border.getOrCompute()
        if (insets == Insets.ZERO) return
        
        val borderZone = element.borderZone
        val width = borderZone.width.getOrCompute()
        val height = borderZone.height.getOrCompute()
        if (width <= 0f || height <= 0f) return
        
        val x = originX + borderZone.x.getOrCompute()
        val y = originY - borderZone.y.getOrCompute()
        val lastColor = batch.packedColor
        val thisColor = this.color.getOrCompute()
        val opacity = element.apparentOpacity.getOrCompute()
        val tmpColor = ColorStack.getAndPush().set(thisColor)
        tmpColor.a *= opacity
        batch.color = tmpColor
        
        batch.fillRect(x, y - height, insets.left, height)
        batch.fillRect(x + width - insets.right, y - height, insets.right, height)
        val topBottomWidth = width - insets.left - insets.right
        batch.fillRect(x + insets.left, y - height, topBottomWidth, insets.bottom)
        batch.fillRect(x + insets.left, y - insets.top, topBottomWidth, insets.top)
        
        ColorStack.pop()
        batch.packedColor = lastColor
    }
    
}