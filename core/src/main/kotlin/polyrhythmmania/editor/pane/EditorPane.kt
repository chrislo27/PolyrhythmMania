package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Disposable
import paintbox.Paintbox
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.Tooltip
import paintbox.ui.UIElement
import paintbox.ui.animation.Animation
import paintbox.ui.area.Insets
import paintbox.ui.element.RectElement
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.Palette
import polyrhythmmania.editor.help.HelpDialog
import polyrhythmmania.editor.pane.dialog.*
import polyrhythmmania.editor.pane.track.AllTracksPane
import polyrhythmmania.ui.DialogPane


class EditorPane(val editor: Editor) : Pane(), Disposable {

    val main: PRManiaGame = editor.main
    val palette: Palette = Palette(main)
    
    private val measurePartCache: MutableMap<Int, Int> = mutableMapOf()

    val statusBarMsg: Var<String> = Var("")

    val bgRect: RectElement
    val menubar: Menubar
    val statusBar: StatusBar
    val upperPane: UpperPane
    val allTracksPane: AllTracksPane

    val toolbar: Toolbar get() = upperPane.toolbar
    val musicDialog: MusicDialog
    val exitConfirmDialog: ExitConfirmDialog
    val settingsDialog: SettingsDialog
    var helpDialog: HelpDialog
        private set // TODO make this a val again, var is for fast-resetting for debugging
    val saveDialog: SaveDialog
    val loadDialog: LoadDialog
    val newDialog: NewDialog

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

    init {
        musicDialog = MusicDialog(this)
        exitConfirmDialog = ExitConfirmDialog(this)
        settingsDialog = SettingsDialog(this)
        helpDialog = HelpDialog(this)
        saveDialog = SaveDialog(this)
        loadDialog = LoadDialog(this)
        newDialog = NewDialog(this)
    }
    
    fun resetHelpDialog() {
        closeDialog()
        val old = helpDialog
        helpDialog = HelpDialog(this)
        old.disposeQuietly()
        Paintbox.LOGGER.debug("Reset help dialog")
    }
    
    fun getMeasurePart(beat: Int): Int {
        return measurePartCache.getOrPut(beat) { editor.engine.timeSignatures.getMeasurePart(beat.toFloat()) }
    }

    fun createDefaultTooltip(binding: Var.Context.() -> String): Tooltip {
        return Tooltip(binding = binding).apply {
            this.markup.set(palette.markup)
        }
    }

    fun createDefaultTooltip(str: ReadOnlyVar<String>): Tooltip {
        return createDefaultTooltip { str.use() }
    }

    fun createDefaultTooltip(str: String): Tooltip {
        return createDefaultTooltip { str }
    }

    fun createAnimation(start: Float, end: Float, duration: Float = 0.125f,
                        interpolation: Interpolation = Interpolation.smoother): Animation {
        return Animation(interpolation, duration, start, end)
    }

    fun enqueueAnimation(animation: Animation, varr: FloatVar) {
        sceneRoot.getOrCompute()?.animations?.enqueueAnimation(animation, varr)
    }

    fun enqueueAnimation(varr: FloatVar, start: Float, end: Float, duration: Float = 0.125f,
                         interpolation: Interpolation = Interpolation.smoother): Animation {
        val animation = Animation(interpolation, duration, start, end)
        enqueueAnimation(animation, varr)
        return animation
    }

    fun styleIndentedButton(button: IndentedButton) {
        button.indentedButtonBorderColor.bind { palette.toolbarIndentedButtonBorderTint.use() }
    }

    fun closeDialog() {
        val root = sceneRoot.getOrCompute()
        if (root != null) {
            root.hideRootContextMenu()
            root.hideRootDialog()
        }
    }

    fun openDialog(dialog: DialogPane) {
        closeDialog()
        val sceneRoot = sceneRoot.getOrCompute()
        if (sceneRoot != null) {
            sceneRoot.showRootDialog(dialog)
            enqueueAnimation(dialog.opacity, 0f, 1f).apply {
                onStart = { dialog.visible.set(true) }
            }
        }
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelfAfterChildren(originX, originY, batch)
        measurePartCache.clear()
    }

    override fun dispose() {
        helpDialog.dispose()
    }
}