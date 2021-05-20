package polyrhythmmania.editor.pane

import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.UIElement
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.element.RectElement
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.Palette
import polyrhythmmania.editor.pane.track.AllTracksPane


class EditorPane(val editor: Editor) : Pane() {

    val main: PRManiaGame = editor.main
    val palette: Palette = Palette(main)
    
    val statusBarMsg: Var<String> = Var("")
    
    val bgRect: RectElement
    val menubar: Menubar
    val statusBar: StatusBar
    val upperPane: UpperPane
    val allTracksPane: AllTracksPane
    
    val toolbar: Toolbar get() = upperPane.toolbar
    
    init {
        // Background
        bgRect = RectElement().apply { 
            this.color.bind { palette.bgColor.use() }
        }
        this += bgRect // Full pane area
        
        val parent: UIElement = bgRect
        
        // Menubar
        val menubarBacking = RectElement().apply { 
            this.color.bind { palette.toolbarBg.use() }
            Anchor.TopLeft.configure(this)
            this.bounds.height.set(40f)
        }
        parent += menubarBacking
        menubar = Menubar(this).apply {
            Anchor.TopLeft.configure(this)
            this.padding.set(Insets(4f))
        }
        menubarBacking += menubar
        
        statusBar = StatusBar(this).apply {
            Anchor.BottomCentre.configure(this)
            this.bindWidthToParent()
            this.bounds.height.set(24f)
        }
        parent += statusBar
        
        upperPane = UpperPane(this).apply {
            Anchor.TopLeft.configure(this, offsetY = { ctx -> ctx.use(menubarBacking.bounds.height) })
            this.bounds.height.bind { 
                (300 * ((sceneRoot.use()?.bounds?.height?.use() ?: 720f) / 720f)).coerceAtLeast(300f)
            }
        }
        parent += upperPane
        
        allTracksPane = AllTracksPane(this).apply {
            Anchor.TopLeft.configure(this)
            this.bounds.y.bind { 
                upperPane.bounds.y.use() + upperPane.bounds.height.use()
            }
            this.bounds.height.bind { 
                statusBar.bounds.y.use() - bounds.y.use()
            }
        }
        parent += allTracksPane
    }
    
}