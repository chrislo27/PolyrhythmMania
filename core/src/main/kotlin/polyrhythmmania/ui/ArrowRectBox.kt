package polyrhythmmania.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.registry.AssetRegistry
import paintbox.ui.ImageNode
import paintbox.ui.UIElement
import paintbox.ui.element.QuadElement
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox


/**
 * Used in Endless Mode lives indicator.
 */
class ArrowRectBox(
        val contentHbox: HBox,
        val black: Color = Color(0f, 0f, 0f, 0.5f)
) : HBox() {
    
    val bg: RectElement = RectElement(black).apply {
        this.bounds.width.eagerBind { contentHbox.bounds.width.use() }
        this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_triangle_equilateral"))).apply {
            this.bindWidthToSelfHeight()
            this.tint.set(black)
            this.rotation.set(-90f)
            this.bindXToParentWidth()
        }
        this += contentHbox
    }
    val fadeQuad: UIElement = QuadElement(black).apply {
        this.bounds.width.set(32f)
        this.leftRightGradient(black.cpy().also { it.a *= 0.5f }, black)
    }
    
    init {
        this.autoSizeToChildren.set(true)
        this.autoSizeMinimumSize.set(1f)
        this.spacing.set(0f)
        
        this.temporarilyDisableLayouts {
            this += fadeQuad
            this += bg
        }
    }
}
