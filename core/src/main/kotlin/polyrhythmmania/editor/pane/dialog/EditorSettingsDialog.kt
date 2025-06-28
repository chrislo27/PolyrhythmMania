package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.Paintbox
import paintbox.PaintboxGame
import paintbox.binding.*
import paintbox.font.PaintboxFont
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.Settings
import polyrhythmmania.editor.CameraPanningSetting
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.screen.mainmenu.menu.StandardMenu
import polyrhythmmania.ui.PRManiaSkins
import kotlin.math.max


typealias ItemToStringBinding<T> = VarContext.(item: T) -> String

class EditorSettingsDialog(editorPane: EditorPane) : EditorDialog(editorPane) {
    
    val settings: Settings = editorPane.main.settings
    
    private val uiScaleListener: VarChangedListener<Int> = VarChangedListener {
        Gdx.app.postRunnable {
            editor.resize()
        }
    }

    init {
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.settings.title").use() }

        bottomPane.addChild(Button("").apply {
            Anchor.BottomRight.configure(this)
            this.bindWidthToSelfHeight()
            this.applyDialogStyleBottom()
            this.setOnAction {
                attemptClose()
            }
            this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor_linear")["x"])).apply {
                this.tint.bind { editorPane.palette.toolbarIconToolNeutralTint.use() }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("common.close")))
        })
        
        val resetDefaultActions: MutableList<() -> Unit> = mutableListOf()
        bottomPane.addChild(HBox().apply { 
            this.spacing.set(8f)
            this.bindWidthToParent(adjust = -100f)
            this += Button(binding = { Localization.getVar("editor.dialog.settings.resetAllToDefault").use() }, font = editorPane.palette.musicDialogFont).apply { 
                this.applyDialogStyleBottom()
                this.bounds.width.set(450f)
                this.setOnAction { 
                    resetDefaultActions.forEach { it.invoke() }
                }
            }
        })

        val scrollPane: ScrollPane = ScrollPane().apply {
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBar.unitIncrement.set(64f)
            this.vBar.blockIncrement.set(100f)
            this.vBar.skinID.set(PRManiaSkins.SCROLLBAR_SKIN)
        }
        contentPane.addChild(scrollPane)

        val pane = Pane()
        val vbox = VBox().apply {
            this.spacing.set(8f)
            this.margin.set(Insets(0f, 0f, 0f, 4f))
        }
        pane += vbox
        val blockHeight = 64f

        fun createCheckbox(text: String, tooltip: String?, keyValue: Settings.KeyValue<Boolean>): CheckBox {
            val bindingVar = keyValue.value
            return CheckBox(binding = { Localization.getVar(text).use() }, font = editorPane.palette.musicDialogFont).apply {
                this.color.set(Color.WHITE.cpy())
                this.bounds.height.set(blockHeight)
                if (tooltip != null) {
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar(tooltip)))
                }
                this.imageNode.padding.set(Insets(8f))
                this.checkedState.set(bindingVar.getOrCompute())
                this.checkedState.addListener {
                    bindingVar.set(it.getOrCompute())
                }
                @Suppress("MoveLambdaOutsideParentheses")
                resetDefaultActions.add({
                    this.checkedState.set(keyValue.defaultValue)
                })
            }
        }

        fun createGenericPane(
                text: String, tooltip: String?, child: UIElement,
                font: PaintboxFont = editorPane.palette.musicDialogFont,
                percentageContent: Float = 0.5f,
        ): Pane {
            return Pane().apply {
                this.bounds.height.set(blockHeight)
                addChild(TextLabel(binding = { Localization.getVar(text).use() }, font = font).apply {
                    Anchor.TopLeft.configure(this)
                    this.textColor.set(Color.WHITE)
                    this.renderAlign.set(Align.left)
                    this.bindWidthToParent(multiplier = 1f - percentageContent)
                    if (tooltip != null) {
                        this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar(tooltip)))
                    }
                })
                addChild(child.apply {
                    Anchor.TopRight.configure(this)
                    this.bindWidthToParent(multiplier = percentageContent)
                })
            }
        }

        fun <T> createCycleOption(text: String, tooltip: String?, keyValue: Settings.KeyValue<T>,
                                  items: List<T>,
                                  font: PaintboxFont = editorPane.palette.musicDialogFont,
                                  percentageContent: Float = 0.5f,
                                  itemToStringBinding: ItemToStringBinding<T>? = null): Pair<Pane, CycleControl<T>> {
            val bindingVar: Var<T> = keyValue.value
            val cycle = CycleControl(items, bindingVar, itemToStringBinding)
            @Suppress("MoveLambdaOutsideParentheses")
            resetDefaultActions.add({
                cycle.currentItem.set(keyValue.defaultValue)
            })
            return createGenericPane(text, tooltip, cycle, font, percentageContent) to cycle
        }

        fun createSlider(text: String, tooltip: String?, slider: Slider,
                         font: PaintboxFont = editorPane.palette.musicDialogFont,
                         percentageContent: Float = 0.5f): Pair<Pane, Slider> {
            return createGenericPane(text, tooltip, slider, font, percentageContent) to slider
        }

        vbox.temporarilyDisableLayouts {
            vbox += createCheckbox("editorSettings.detailedMarkerUndo", "editorSettings.detailedMarkerUndo.tooltip", settings.kv_editorDetailedMarkerUndo)
            vbox += createCheckbox("editorSettings.cameraPanOnDragEdge", "editorSettings.cameraPanOnDragEdge.tooltip", settings.kv_editorCameraPanOnDragEdge)
            vbox += createCheckbox("editorSettings.higherAccuracyPreview", "editorSettings.higherAccuracyPreview.tooltip", settings.kv_editorHigherAccuracyPreview)
            vbox += createCheckbox("editorSettings.playtestStartsPlay", "editorSettings.playtestStartsPlay.tooltip", settings.kv_editorPlaytestStartsPlay)
            vbox += createCheckbox("editorSettings.arrowKeysLikeScroll", "editorSettings.arrowKeysLikeScroll.tooltip", settings.kv_editorArrowKeysLikeScroll)
            vbox += createCycleOption("editorSettings.cameraPanningSetting", "editorSettings.cameraPanningSetting.tooltip",
                    settings.kv_editorPanningDuringPlayback, CameraPanningSetting.entries,
                    itemToStringBinding = { Localization.getVar(it.localization).use() }).first
            vbox += createCycleOption("editorSettings.autosaveInterval", "editorSettings.autosaveInterval.tooltip",
                    settings.kv_editorAutosaveInterval, Editor.AUTOSAVE_INTERVALS,
                    itemToStringBinding = { item ->
                        Localization.getVar("editorSettings.autosaveInterval.minutes", Var {
                            listOf(item)
                        }).use()
                    }).first
            vbox += createGenericPane("editorSettings.musicWaveformOpacity", "editorSettings.musicWaveformOpacity.tooltip",
                    Pane().also { pane ->
                        val keyValue = settings.kv_editorMusicWaveformOpacity
                        val bindingVar = keyValue.value
                        
                        val slider = Slider().also { slider ->
                            Anchor.CentreLeft.configure(slider)
                            slider.bindWidthToParent(adjust = -100f)
                            slider.bounds.height.set(32f)
                            slider.minimum.set(0f)
                            slider.maximum.set(10f)
                            slider.tickUnit.set(1f)
                            slider.setValue(bindingVar.getOrCompute().toFloat())
                            slider.value.addListener { sliderValue ->
                                bindingVar.set(sliderValue.getOrCompute().toInt())
                            }
                            
                            @Suppress("MoveLambdaOutsideParentheses")
                            resetDefaultActions.add({
                                slider.setValue(keyValue.defaultValue.toFloat())
                            })
                        }
                        pane += slider
                        pane += TextLabel(binding = { "${bindAndGet(bindingVar) * 10}%" },
                                font = editorPane.palette.musicDialogFont).apply {
                            Anchor.TopRight.configure(this)
                            this.bounds.width.set(100f)
                            this.padding.set(Insets(4f))
                            this.textColor.set(Color.WHITE)
                            this.renderAlign.set(Align.center)
                        }
                    }, percentageContent = 0.5f)
            val (uiScalePane, uiScaleControl) = createCycleOption("editorSettings.uiScale", "editorSettings.uiScale.tooltip",
                    settings.kv_editorUIScale, listOf(1, 2, 3, 4),
                    itemToStringBinding = { if (it == 1) Localization.getValue("editorSettings.uiScale.native") else "$it" })
            uiScaleControl.currentItem.addListener(WeakVarChangedListener(uiScaleListener))
            vbox += uiScalePane
        }
        vbox.sizeHeightToChildren(300f)

        pane.bounds.height.bind {
            max(vbox.bounds.height.use(), pane.parent.use()?.bounds?.height?.use() ?: 300f)
        }
        scrollPane.setContent(pane)
    }

    override fun canCloseDialog(): Boolean {
        return true
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
        settings.persist()
        Paintbox.LOGGER.info("Settings persisted (editor)")
    }

    private inner class CycleControl<T>(val list: List<T>, bindingVar: Var<T>,
                                        itemToStringBinding: ItemToStringBinding<T>? = null)
        : Pane(), HasPressedState by HasPressedState.DefaultImpl() {

        val left: Button
        val right: Button
        val label: TextLabel

        val currentItem: Var<T> = bindingVar
        val itemToString: ReadOnlyVar<String> = if (itemToStringBinding != null) {
            Var.bind { itemToStringBinding.invoke(this, currentItem.use()) }
        } else {
            Var.bind {
                currentItem.use().toString()
            }
        }

        init {
            this.margin.set(Insets(8f))
            left = Button("").apply {
                Anchor.CentreLeft.configure(this)
                this.bindWidthToSelfHeight()
                this.skinID.set(StandardMenu.BUTTON_LONG_SKIN_ID)
                addChild(ImageNode(TextureRegion(PaintboxGame.gameInstance.staticAssets.paintboxSpritesheet.upArrow)).apply {
                    this.rotation.set(90f)
                    this.padding.set(Insets(10f))
                    this.tint.set(Color.WHITE)
                })
                this.setOnAction {
                    val index = list.indexOf(currentItem.getOrCompute())
                    val nextIndex = (index - 1 + list.size) % list.size
                    currentItem.set(list[nextIndex])
                }
            }
            right = Button("").apply {
                Anchor.CentreRight.configure(this)
                this.bindWidthToSelfHeight()
                this.skinID.set(StandardMenu.BUTTON_LONG_SKIN_ID)
                addChild(ImageNode(TextureRegion(PaintboxGame.gameInstance.staticAssets.paintboxSpritesheet.upArrow)).apply {
                    this.rotation.set(270f)
                    this.padding.set(Insets(10f))
                    this.tint.set(Color.WHITE)
                })
                this.setOnAction {
                    val index = list.indexOf(currentItem.getOrCompute())
                    val nextIndex = (index + 1) % list.size
                    currentItem.set(list[nextIndex])
                }
            }
            label = TextLabel(binding = { itemToString.use() }, font = editorPane.palette.musicDialogFont).apply {
                Anchor.Centre.configure(this)
                this.bindWidthToParent { -(bounds.height.use() * 2) }
                this.textColor.set(Color.WHITE)
                this.textAlign.set(TextAlign.CENTRE)
                this.renderAlign.set(Align.center)
                this.markup.set(editorPane.palette.markupInstantiatorDesc)
            }

            addChild(left)
            addChild(right)
            addChild(label)

            @Suppress("LeakingThis")
            HasPressedState.DefaultImpl.addDefaultPressedStateInputListener(this)
        }
    }
}