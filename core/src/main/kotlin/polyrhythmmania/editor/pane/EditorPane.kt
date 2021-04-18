package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.Color
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.element.RectElement
import io.github.chrislo27.paintbox.util.gdxutils.grey


class EditorPane : Pane() {
    
    val toolbar: Toolbar
    
    init {
        val bgColor = Color().grey(0.094f)
        val backColor1 = Color().grey(0.3f)
        
        // Background
        this += RectElement(bgColor) // Full pane area
        
        // Toolbar
        val toolbarBacking = RectElement(backColor1).apply { 
            Anchor.TopLeft.configure(this)
            this.bounds.height.set(40f)
        }
        this += toolbarBacking
        toolbar = Toolbar().apply {
            Anchor.Centre.configure(this)
            val padding = 4f
            this.bindWidthToParent(-padding * 2)
            this.bindHeightToParent(-padding * 2)
        }
        toolbarBacking += toolbar
    }
    
}