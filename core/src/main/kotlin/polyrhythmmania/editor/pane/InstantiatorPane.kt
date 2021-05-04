package polyrhythmmania.editor.pane

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.ClickPressed
import io.github.chrislo27.paintbox.ui.ImageNode
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.control.ButtonSkin
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.element.RectElement
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.track.block.Instantiator
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
    
    init {
    }
    
}

class InstantiatorList(val instantiatorPane: InstantiatorPane) : Pane() {

    val upperPane: UpperPane = instantiatorPane.upperPane
    val editorPane: EditorPane = instantiatorPane.editorPane
    val editor: Editor = instantiatorPane.editor
    
    val buttonPane: Pane
    val listPane: Pane
    
    init {
        val buttonWidth = 32f
        
        buttonPane = Pane().apply {
            this.bounds.width.set(buttonWidth + 2f)
            this.margin.set(Insets(0f, 0f, 0f, 2f))
        }
        this += buttonPane
        buttonPane += Button("").apply {
            this.padding.set(Insets.ZERO)
            Anchor.TopLeft.configure(this)
            this.bounds.width.set(buttonWidth)
            this.bounds.height.set(buttonWidth)
            this.skinID.set(EditorSkins.BUTTON_NO_SKIN)
            this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_icon_buttons_editor"), 16 * 3, 16 * 3, 16, 16))
        }
        buttonPane += Button("").apply {
            this.padding.set(Insets.ZERO)
            Anchor.BottomLeft.configure(this)
            this.bounds.width.set(buttonWidth)
            this.bounds.height.set(buttonWidth)
            this.skinID.set(EditorSkins.BUTTON_NO_SKIN)
            this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_icon_buttons_editor"), 16 * 3, 16 * 3, 16, 16)).apply {
                rotation.set(180f)
            }
        }
        buttonPane += Button("").apply {
            this.padding.set(Insets.ZERO)
            Anchor.CentreLeft.configure(this)
            this.bounds.width.set(buttonWidth)
            this.bounds.height.set(buttonWidth)
            this.skinID.set(EditorSkins.BUTTON_NO_SKIN)
            this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_icon_buttons_editor"), 16 * 6, 16 * 4, 16, 16))
        }
        
        listPane = Pane().apply {
            bounds.x.set(buttonWidth + 2f)
            bindWidthToParent(-(buttonWidth + 2f))
        }
        this += listPane
        // FIXME remove
        val first = Instantiators.list.first()
        listPane += TextLabel(binding = { first.name.use() }, font = editorPane.palette.instantiatorFont).apply { 
            renderAlign.set(Align.left)
            textAlign.set(TextAlign.LEFT)
            textColor.set(Color.WHITE)
        }
    }
    
    init {
        listPane.addInputEventListener { event ->
            when (event) {
                is ClickPressed -> {
                    if (event.button == Input.Buttons.LEFT) {
                        editor.attemptInstantiatorDrag(this.getCurrentInstantiator())
                        true
                    } else false
                }
                else -> false
            }
        }
    }
    
    fun getCurrentInstantiator(): Instantiator {
        // TODO
        return Instantiators.list.first()
    }
    
}