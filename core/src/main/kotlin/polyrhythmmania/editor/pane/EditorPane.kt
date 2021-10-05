package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.IntIntMap
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
import kotlin.math.roundToInt


class EditorPane(val editor: Editor) : Pane(), Disposable {

    val main: PRManiaGame = editor.main
    val palette: Palette = Palette(main)
    
    private val measurePartCache: IntIntMap = IntIntMap()

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
        private set // var is for fast-resetting for debugging
    val saveDialog: SaveDialog
    val loadDialog: LoadDialog
    val newDialog: NewDialog
    val playtestDialog: PlaytestDialog
    val paletteEditDialog: PaletteEditDialog
    val texturePackEditDialog: TexturePackEditDialog

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
            Anchor.TopLeft.configure(this, offsetY = { menubarBacking.bounds.height.useF() })
            this.bounds.height.bind {
                (300 * ((sceneRoot.use()?.bounds?.height?.useF() ?: 720f) / 720f)).coerceAtLeast(300f)
            }
        }
        parent += upperPane
        // Order matters here, related to autosaving text.
        parent += menubarBacking

        allTracksPane = AllTracksPane(this).apply {
            Anchor.TopLeft.configure(this)
            this.bounds.y.bind {
                upperPane.bounds.y.useF() + upperPane.bounds.height.useF()
            }
            this.bounds.height.bind {
                statusBar.bounds.y.useF() - bounds.y.useF()
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
        playtestDialog = PlaytestDialog(this)
        paletteEditDialog = PaletteEditDialog(this, editor.container.world.tilesetPalette, null, false)
        texturePackEditDialog = TexturePackEditDialog(this)
    }
    
    fun resetHelpDialog() {
        closeDialog()
        val old = helpDialog
        helpDialog = HelpDialog(this)
        old.disposeQuietly()
        Paintbox.LOGGER.debug("Reset help dialog")
    }
    
    fun getMeasurePart(beat: Int): Int {
        val get = measurePartCache.get(beat, -999)
        return if (get == -999) {
            val mp = editor.engine.timeSignatures.getMeasurePart(beat.toFloat())
            measurePartCache.put(beat, mp)
            mp
        } else get
    }
    
    fun shouldDrawBeatLine(trackViewScale: Float, beat: Int, measurePart: Int, subbeat: Boolean): Boolean {
        if (subbeat) {
            if (trackViewScale <= 0.45f) return false
        } else {
            if (measurePart != 0 && trackViewScale <= 0.35f) {
                if (measurePart > 0) {
                    if (trackViewScale <= 0.25f) {
                        if (measurePart % ((1f / trackViewScale).roundToInt()) != 0) return false
                    } else {
                        if (beat % 2 != 0) return false
                    }
                } else {
                    if (trackViewScale <= 0.25f) {
                        if (beat % 4 != 0) return false
                    } else {
                        if (beat % 2 != 0) return false
                    }
                }
            }
        }
        return true
    }

    fun shouldDrawBeatNumber(trackViewScale: Float, beat: Int, measurePart: Int): Boolean {
        if (measurePart != 0 && trackViewScale <= 0.55f) {
            if (measurePart > 0) {
                if (trackViewScale <= 0.25f) {
                    return false
                } else {
                    if (measurePart % 2 != 0) return false
                }
            } else {
                if (beat % 4 != 0) return false
            }
        }
        return true
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

    fun createDefaultBarSeparator(): UIElement {
        return RectElement(binding = { palette.previewPaneSeparator.use() }).apply {
            this.bounds.width.set(2f)
            this.margin.set(Insets(2f, 2f, 0f, 0f))
        }
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