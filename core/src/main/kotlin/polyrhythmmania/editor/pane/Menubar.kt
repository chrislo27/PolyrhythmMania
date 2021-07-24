package polyrhythmmania.editor.pane

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.animation.Animation
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.util.gdxutils.isAltDown
import paintbox.util.gdxutils.isControlDown
import paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import kotlin.math.roundToInt


class Menubar(val editorPane: EditorPane) : Pane() {
    
    val editor: Editor = editorPane.editor
    
    val ioNew: Button
    val ioOpen: Button
    val ioSave: Button
    val undoButton: Button
    val redoButton: Button
    
    val helpButton: Button
    val settingsButton: Button
    val exitButton: Button
    
    private val autosavePane: Pane
    private val autosaveIndicator: Tooltip
    private val currentTimeSec: Var<Long> = Var(-1L)
    
    init {
        fun separator(): UIElement {
            return editorPane.createDefaultBarSeparator()
        }
        
        val leftBox: HBox = HBox().apply { 
            this.spacing.set(4f)
            this.align.set(HBox.Align.LEFT)
            this.bounds.width.set((32f + this.spacing.get()) * 6)
        }
        this.addChild(leftBox)
        val rightBox: HBox = HBox().apply {
            Anchor.TopRight.configure(this)
            this.spacing.set(4f)
            this.align.set(HBox.Align.RIGHT)
            this.bounds.width.set((32f + this.spacing.get()) * 3)
        }
        this.addChild(rightBox)
        
        ioNew = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_new"]))
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.new")))
            this.setOnAction {
                editor.attemptNewLevel()
            }
        }
        ioOpen = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_open"]))
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.open")))
            this.setOnAction {
                editor.attemptLoad(null)
            }
        }
        ioSave = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_save"]))
            val tooltipArgsVar: ReadOnlyVar<List<Any?>> = Var {
                val ms = editor.lastAutosaveTimeMs.use()
                val autosaveInterval = editor.autosaveInterval.use()
                currentTimeSec.use()
                if (ms <= 0 || autosaveInterval <= 0) {
                    listOf(Localization.getValue("editor.button.save.time.never"))
                } else {
                    listOf(Localization.getValue("editor.button.save.time",
                            ((System.currentTimeMillis() - ms) / 60_000f).roundToInt()))
                } + listOf(Localization.getValue("editorSettings.autosaveInterval.minutes", autosaveInterval))
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.save", tooltipArgsVar)))
            this.setOnAction { 
                val control = Gdx.input.isControlDown()
                val alt = Gdx.input.isAltDown()
                val shift = Gdx.input.isShiftDown()
                editor.attemptSave(forceSaveAs = !control && alt && !shift)
            }
        }
        undoButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            val active: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_undo"])
            val inactive: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_undo_white"])
            this += ImageNode(null).apply {
                this.textureRegion.bind {
                    if (apparentDisabledState.use()) inactive else active
                }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.undo")))
            this.disabled.bind { editor.undoStackSize.use() <= 0 }
            this.setOnAction { 
                editor.attemptUndo()
            }
        }
        redoButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            val active: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_undo"]).apply { flip(true, false) }
            val inactive: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_undo_white"]).apply { flip(true, false) }
            this += ImageNode(null).apply {
                this.textureRegion.bind {
                    if (apparentDisabledState.use()) inactive else active
                }
//                this.tint.bind {
//                    if (apparentDisabledState.use()) Color.GRAY else Color.WHITE
//                }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.redo")))
            this.disabled.bind { editor.redoStackSize.use() <= 0 }
            this.setOnAction {
                editor.attemptRedo()
            }
        }
        
        helpButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["help"])).apply { 
                this.tint.bind { editorPane.palette.menubarIconTint.use() }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.help")))
            this.setOnAction {
                editor.attemptOpenHelpDialog()
            }
            this.setOnRightClick { // DEBUG
                if (Paintbox.debugMode) {
                    editorPane.resetHelpDialog()
                }
            }
        }
        settingsButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["settings"]))
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.settings")))
            this.setOnAction {
                editor.attemptOpenSettingsDialog()
            }
        }
        exitButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["menubar_exit"]))
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.exit")))
            this.setOnAction {
                editor.attemptExitToTitle()
            }
        }
        
        leftBox.temporarilyDisableLayouts {
            leftBox += ioNew
            leftBox += ioOpen
            leftBox += ioSave
            leftBox += separator()
            leftBox += undoButton
            leftBox += redoButton
        }
        rightBox.temporarilyDisableLayouts { 
            rightBox += helpButton
            rightBox += settingsButton
            rightBox += separator()
            rightBox += exitButton
        }
        
        autosavePane = Pane().apply {
            this.bounds.y.bind { parent.use()?.bounds?.height?.useF() ?: 0f }
            this.doClipping.set(true)
        }
        autosaveIndicator = editorPane.createDefaultTooltip("").apply { 
            Anchor.BottomLeft.configure(this)
            this.setScaleXY(0.8f)
            this.renderAlign.set(Align.bottomLeft)
        }
        autosavePane += autosaveIndicator
        ioSave += autosavePane
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelf(originX, originY, batch)
        currentTimeSec.set(System.currentTimeMillis() / 1000)
    }

    fun triggerAutosaveIndicator(text: String) {
        val sceneRoot = sceneRoot.getOrCompute() ?: return
        val animations = sceneRoot.animations
        val varr = autosavePane.bounds.height
        animations.cancelAnimationFor(varr)
        
        autosaveIndicator.text.set(text)
        autosaveIndicator.resizeBoundsToContent()
        val height = autosaveIndicator.bounds.height.get()
        val width = autosaveIndicator.bounds.width.get()
        autosavePane.bounds.width.set(width)
        
        autosavePane.visible.set(true)
        val interpolation = Interpolation.smoother
        animations.enqueueAnimation(Animation(interpolation, 0.25f, 0f, height).also { ani1 ->
            ani1.onComplete = {
                Gdx.app.postRunnable {
                    animations.enqueueAnimation(Animation(interpolation, ani1.duration, height, 0f, delay = 5f).apply {
                        this.onComplete = {
                            autosavePane.visible.set(false)
                        }
                    }, varr)
                }
            }                                                                                                         
        }, varr)
    }
}