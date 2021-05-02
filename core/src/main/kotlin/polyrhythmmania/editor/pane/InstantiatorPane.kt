package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.element.RectElement
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.track.block.Instantiators
import kotlin.math.roundToInt


class InstantiatorPane(val upperPane: UpperPane) : Pane() {
    
    val editorPane: EditorPane = upperPane.editorPane
    val editor: Editor = upperPane.editor
    
    val list: InstantiatorList
    
    init {
        val middleDivider = RectElement(binding = { editorPane.palette.instantiatorPaneBorder.use() }).apply {
            Anchor.TopCentre.configure(this)
            this.margin.set(Insets(6f))
            this.bounds.width.bind { 
                val margin = margin.use()
                (2f + margin.left + margin.right).roundToInt().toFloat()
            }
            this.bounds.x.set(300f)
        }
        this += middleDivider
        
        val scrollSelector = Pane().apply {
            this.bounds.width.bind { middleDivider.bounds.x.use() }
        }
        this += scrollSelector
        
        val descPane = Pane().apply {
            Anchor.TopRight.configure(this)
            this.bounds.width.bind { (parent.use()?.contentZone?.width?.use() ?: 0f) - (middleDivider.bounds.x.use() + middleDivider.bounds.width.use()) }
        }
        this += descPane
        
        list = InstantiatorList(this).apply { 
            
        }
        scrollSelector += list
    }
    
}

class InstantiatorList(val instantiatorPane: InstantiatorPane) : Pane() {

    val upperPane: UpperPane = instantiatorPane.upperPane
    val editorPane: EditorPane = instantiatorPane.editorPane
    
    init {
        // FIXME remove
        val first = Instantiators.list.first()
        this += TextLabel(binding = { first.name.use() }, font = editorPane.palette.instantiatorFont).apply { 
            renderAlign.set(Align.left)
            textAlign.set(TextAlign.LEFT)
            textColor.set(Color.WHITE)
        }
    }
    
}