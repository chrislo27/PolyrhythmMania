package paintbox.ui.element

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.util.ColorStack
import paintbox.ui.UIElement
import paintbox.binding.Var
import paintbox.util.gdxutils.fillRect


class RectElement(initColor: Color) : UIElement() {
    
    val color: Var<Color> = Var(Color(1f, 1f, 1f, 1f).set(initColor))
    
    constructor() : this(Color.WHITE)
    
    constructor(binding: Var.Context.() -> Color) : this() {
        color.bind(binding)
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val renderBounds = this.paddingZone
        val x = renderBounds.x.get() + originX
        val y = originY - renderBounds.y.get()
        val w = renderBounds.width.get()
        val h = renderBounds.height.get()
        val lastPackedColor = batch.packedColor

        val opacity: Float = this.apparentOpacity.get()
        val tmpColor: Color = ColorStack.getAndPush()
        tmpColor.set(color.getOrCompute())
        tmpColor.a *= opacity
        batch.color = tmpColor
        batch.fillRect(x, y - h, w, h)

        ColorStack.pop()
        batch.packedColor = lastPackedColor
    }
}