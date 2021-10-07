package paintbox.ui.contextmenu

import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.font.*
import paintbox.ui.UIElement


/**
 * A [MenuItem] is an item used in a [ContextMenu]
 */
sealed class MenuItem {
    companion object {
        val NOTHING_ACTION: () -> Unit = {}
        val NO_TOOLTIP: (Var<UIElement?>) -> Unit = { }
    }
    
    var closeMenuAfterAction: Boolean = true
    var onAction: () -> Unit = NOTHING_ACTION
    
    var createTooltip: (Var<UIElement?>) -> Unit = NO_TOOLTIP
}

/**
 * A [MenuItem] that will spawn a sub-menu with other items.
 */
class Menu(val otherItems: List<MenuItem>) : MenuItem()

/**
 * A [MenuItem] that represents a horizontal separator.
 */
class SeparatorMenuItem() : MenuItem()

/**
 * A [MenuItem] that has a custom [element] as the node. This [element] should NOT depend on its parent
 * bounds for width and height!
 */
class CustomMenuItem(val element: UIElement) : MenuItem()

class LabelMenuItem(val textBlock: TextBlock, val scaleXY: Float = 1f) : MenuItem() {
    companion object {
        fun create(text: String, font: PaintboxFont, scaleXY: Float = 1f): LabelMenuItem {
            return LabelMenuItem(TextRun(font, text).toTextBlock(), scaleXY)
        }

        fun create(text: String, markup: Markup, scaleXY: Float = 1f): LabelMenuItem {
            return LabelMenuItem(markup.parse(text), scaleXY)
        }
    }
    
    var textAlign: TextAlign = TextAlign.LEFT
    var renderAlign: Int = Align.left
}

class SimpleMenuItem(val textBlock: TextBlock, val scaleXY: Float = 1f) : MenuItem() {
    companion object {
        fun create(text: String, font: PaintboxFont, scaleXY: Float = 1f): SimpleMenuItem {
            return SimpleMenuItem(TextRun(font, text).toTextBlock(), scaleXY)
        }

        fun create(text: String, markup: Markup, scaleXY: Float = 1f): SimpleMenuItem {
            return SimpleMenuItem(markup.parse(text), scaleXY)
        }
    }
    
    var interactable: Boolean = true
    
    init {
        this.closeMenuAfterAction = true
    }
}

class CheckBoxMenuItem(val checkState: Var<Boolean>, val textBlock: TextBlock, val scaleXY: Float = 1f) : MenuItem() {

    companion object {
        fun create(checkState: Var<Boolean>, text: String, font: PaintboxFont, scaleXY: Float = 1f): CheckBoxMenuItem {
            return CheckBoxMenuItem(checkState, TextRun(font, text).toTextBlock(), scaleXY)
        }

        fun create(checkState: Var<Boolean>, text: String, markup: Markup, scaleXY: Float = 1f): CheckBoxMenuItem {
            return CheckBoxMenuItem(checkState, markup.parse(text), scaleXY)
        }
    }
    
    init {
        this.closeMenuAfterAction = false
    }
}