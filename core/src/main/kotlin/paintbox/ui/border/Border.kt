package paintbox.ui.border

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.ui.UIElement


interface Border {
    
    fun renderBorder(originX: Float, originY: Float, batch: SpriteBatch, element: UIElement)
    
}