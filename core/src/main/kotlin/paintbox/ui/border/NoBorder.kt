package paintbox.ui.border

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.ui.UIElement


object NoBorder : Border {
    override fun renderBorder(originX: Float, originY: Float, batch: SpriteBatch, element: UIElement) {
        // NO-OP
    }
}