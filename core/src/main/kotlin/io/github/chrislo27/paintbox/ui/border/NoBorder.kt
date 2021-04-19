package io.github.chrislo27.paintbox.ui.border

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.chrislo27.paintbox.ui.UIElement


object NoBorder : Border {
    override fun renderBorder(originX: Float, originY: Float, batch: SpriteBatch, element: UIElement) {
        // NO-OP
    }
}