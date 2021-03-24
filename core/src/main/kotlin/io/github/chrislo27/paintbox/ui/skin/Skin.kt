package io.github.chrislo27.paintbox.ui.skin

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.github.chrislo27.paintbox.ui.UIElement


/**
 * Renders a [Skinnable] [UIElement].
 * Each [Skin] shall only be responsible for rendering one specific [Skinnable].
 */
abstract class Skin<Element : Skinnable<Element>>(val element: Element) {
    
    abstract fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch)
    
    abstract fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch)
    
}

fun interface SkinFactory<S : Skin<Element>, Element : Skinnable<Element>> {
    
    fun createSkin(element: Element): S
    
}
