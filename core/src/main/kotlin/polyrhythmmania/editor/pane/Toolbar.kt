package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.contextmenu.*
import paintbox.ui.control.*
import paintbox.ui.layout.HBox
import polyrhythmmania.Localization
import polyrhythmmania.world.texturepack.TexPackSrcSelectorMenuPane
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.PlayState
import polyrhythmmania.editor.Tool
import polyrhythmmania.editor.pane.dialog.ResultsTextDialog
import paintbox.util.DecimalFormats
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.engine.input.InputTimingRestriction


class Toolbar(val upperPane: UpperPane) : Pane() {

    val editorPane: EditorPane = upperPane.editorPane
    val editor: Editor = editorPane.editor

    val previewSection: Pane
    val mainSection: Pane

    val playtestButton: Button

    val tapalongPane: TapalongPane

    init {
        this.border.set(Insets(2f, 2f, 0f, 0f))
        this.borderStyle.set(SolidBorder().apply { this.color.bind { editorPane.palette.upperPaneBorder.use() } })
        this.padding.set(Insets(2f))

        fun separator(): UIElement {
            return editorPane.createDefaultBarSeparator()
        }

        // Preview section
        previewSection = Pane().apply {
            Anchor.TopLeft.configure(this)
            this.bounds.width.bind { upperPane.previewPane.contentZone.width.use() - 2f }
            this.border.set(Insets(0f, 0f, 0f, 2f))
            this.borderStyle.set(SolidBorder().apply { this.color.bind { editorPane.palette.previewPaneSeparator.use() } })
            this.padding.set(Insets(0f, 0f, 2f, 4f))
        }
        this += previewSection


        val leftPreviewHbox = HBox().apply {
            Anchor.TopLeft.configure(this)
            this.spacing.set(4f)
//            this.bounds.width.set(32f * 4 + this.spacing.get() * 3)
            this.bounds.width.set(160f)
            this.align.set(HBox.Align.LEFT)
        }
        leftPreviewHbox.temporarilyDisableLayouts {
            leftPreviewHbox += Pane().apply {
                this.bounds.width.set(150f)
                val labelVar = Localization.getVar("editor.playbackSpeed", Var {
                    listOf(DecimalFormats.format("0.0#", editor.playbackSpeed.use()))
                })
                this += TextLabel(binding = { labelVar.use() }, font = editorPane.palette.beatTimeFont).apply {
                    this.textColor.set(Color.WHITE)
                    this.bindHeightToParent(multiplier = 0.5f)
                    this.padding.set(Insets(1f))
                    this.setScaleXY(0.65f)
                    this.markup.set(editorPane.palette.markup)
                    this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.playbackSpeed.tooltip")))
                }
                this += Slider().apply {
                    val setSpeeds: List<Float> = listOf(0.25f, 0.30f, 0.35f, 0.40f, 0.45f, 0.50f, 0.60f, 0.70f, 0.75f, 0.80f, 0.85f, 0.90f, 0.95f,
                            1f,
                            1.10f, 1.25f, 1.50f, 1.75f, 2.00f, 2.25f, 2.50f, 2.75f, 3.00f, 4.00f).sorted()
                    val oneIndex: Int = setSpeeds.indexOf(1f)
                    if (oneIndex == -1) error("Didn't find index of speed 1.0 for playback speed slider")
                    
                    Anchor.BottomLeft.configure(this)
                    this.bindHeightToParent(multiplier = 0.5f)
                    this.padding.set(Insets(1f))
                    this.minimum.set(0f)
                    this.maximum.set((setSpeeds.size - 1).toFloat())
                    this.tickUnit.set(1f)
                    this.setValue(oneIndex.toFloat())
                    this.value.addListener { v ->
                        editor.playbackSpeed.set(setSpeeds.getOrNull(v.getOrCompute().toInt()) ?: 1f)
                    }
                    this.setOnRightClick { 
                        this.setValue(oneIndex.toFloat())
                    }
                }
            }
        }
        previewSection += leftPreviewHbox
        val rightPreviewHbox = HBox().apply {
            Anchor.TopRight.configure(this)
            this.spacing.set(4f)
            this.bounds.width.set(32f * 3 + this.spacing.get() * 2)
            this.align.set(HBox.Align.RIGHT)
        }
        previewSection += rightPreviewHbox
        
        playtestButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            val open: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_playtest_open"])
            val shut: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_playtest_shut"])
            this += ImageNode(null).apply {
                this.textureRegion.bind {
                    if (pressedState.use().pressed) shut else open
                }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.playtest")))
            this.setOnAction {
                editor.attemptStartPlaytest()
            }
        }
        rightPreviewHbox.temporarilyDisableLayouts {
            rightPreviewHbox += playtestButton
        }
        
        val playbackButtonPane = HBox().apply {
            Anchor.Centre.configure(this)
            this.spacing.set(4f)
            this.bounds.width.set(32f * 3 + this.spacing.get() * 2)
            this.align.set(HBox.Align.CENTRE)
        }
        previewSection += playbackButtonPane
        val pauseButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            val active: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_pause_color"])
            val inactive: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_pause_white"])
            this += ImageNode(null).apply {
                this.textureRegion.bind {
                    if (apparentDisabledState.use()) inactive else active
                }
                this.tint.bind {
                    if (apparentDisabledState.use()) Color.GRAY else Color.WHITE
                }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.pause")))
            this.disabled.bind { editor.playState.use() != PlayState.PLAYING }
            this.setOnAction {
                editor.changePlayState(PlayState.PAUSED)
            }
        }
        val playButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            val active: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_play_color"])
            val inactive: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_play_white"])
            this += ImageNode(null).apply {
                this.textureRegion.bind {
                    if (apparentDisabledState.use()) inactive else active
                }
                this.tint.bind {
                    if (apparentDisabledState.use()) Color.GRAY else Color.WHITE
                }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.play")))
            this.disabled.bind { editor.playState.use() == PlayState.PLAYING }
            this.setOnAction {
                editor.changePlayState(PlayState.PLAYING)
            }
        }
        val stopButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            val active: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_stop_color"])
            val inactive: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_stop_white"])
            this += ImageNode(null).apply {
                this.textureRegion.bind {
                    if (apparentDisabledState.use()) inactive else active
                }
                this.tint.bind {
                    if (apparentDisabledState.use()) Color.GRAY else Color.WHITE
                }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.stop")))
            this.disabled.bind { editor.playState.use() == PlayState.STOPPED }
            this.setOnAction {
                editor.changePlayState(PlayState.STOPPED)
            }
        }
        playbackButtonPane.temporarilyDisableLayouts {
            playbackButtonPane += pauseButton
            playbackButtonPane += playButton
            playbackButtonPane += stopButton
        }
        previewSection += createPlaybackButtonSet().apply { 
            Anchor.Centre.configure(this)
        }


        // Main section
        mainSection = Pane().apply {
            Anchor.TopRight.configure(this)
            this.bounds.width.bind {
                (parent.use()?.contentZone?.width?.use() ?: 0f) - previewSection.bounds.width.use()
            }
            this.margin.set(Insets(0f, 0f, 4f, 4f))
        }
        this += mainSection

        val tools = Tool.VALUES
        val toolsPane = HBox().apply {
            Anchor.TopRight.configure(this)
            this.align.set(HBox.Align.RIGHT)
            this.spacing.set(4f)
            this.bounds.width.set((32f + this.spacing.get()) * tools.size)
        }
        mainSection += toolsPane
        toolsPane.temporarilyDisableLayouts {
            tools.forEachIndexed { index, thisTool ->
                toolsPane.addChild(IndentedButton("").apply {
                    this.bounds.width.set(32f)
                    this.skinID.set(EditorSkins.BUTTON)
                    this.padding.set(Insets.ZERO)
                    this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_tool")[thisTool.textureKey]))
                    editorPane.styleIndentedButton(this)
                    this.selectedState.bind { editor.tool.use() == thisTool }
                    this.setOnAction {
                        editor.changeTool(thisTool)
                    }
                    val tooltipStr = Localization.getVar("tool.tooltip", Var.bind {
                        listOf(Localization.getVar(thisTool.localizationKey).use(), "${index + 1}")
                    })
                    this.tooltipElement.set(editorPane.createDefaultTooltip(tooltipStr))
                })
            }
        }


        tapalongPane = TapalongPane(this).apply {
            this.bounds.width.set(300f)
        }
        val leftControlHBox = HBox().apply {
            Anchor.TopLeft.configure(this)
            this.align.set(HBox.Align.LEFT)
            this.spacing.set(4f)
            this.bounds.width.set((32f + this.spacing.get()) * 8 + tapalongPane.bounds.width.get())
        }
        mainSection += leftControlHBox

        leftControlHBox.temporarilyDisableLayouts {
            leftControlHBox += Button("").apply {
                this.padding.set(Insets.ZERO)
                this.bounds.width.set(32f)
                this.skinID.set(EditorSkins.BUTTON)
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_texture_pack"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.changeTexturePack")))
                this.setOnAction {
                    if (editor.allowedToEdit.get()) {
                        editor.attemptOpenGenericContextMenu(ContextMenu().also { ctxmenu ->
                            ctxmenu.defaultWidth.set(380f)
                            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("editor.button.changeTexturePack"), editorPane.main.mainFontBold))
//                            ctxmenu.addMenuItem(SeparatorMenuItem())
                            ctxmenu.addMenuItem(CustomMenuItem(TexPackSrcSelectorMenuPane(editorPane,
                                    editor.container.texturePackSource.getOrCompute()) { newSrc ->
                                val container = editor.container
                                container.texturePackSource.set(newSrc)
                                container.setTexturePackFromSource()
                            }))
                            ctxmenu.addMenuItem(SeparatorMenuItem())
                            ctxmenu.addMenuItem(SimpleMenuItem.create(Localization.getValue("editor.button.changeTexturePack.edit"), editorPane.palette.markup).apply {
                                this.onAction = {
                                    editor.attemptOpenManageTexturePacksDialog()
                                }
                            })
                        })
                    }
                }
            }
            leftControlHBox += Button("").apply {
                this.padding.set(Insets.ZERO)
                this.bounds.width.set(32f)
                this.skinID.set(EditorSkins.BUTTON)
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_tileset_palette"]))
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.tilesetPalette")))
                this.setOnAction {
                    editor.attemptOpenPaletteEditDialog()
                }
            }
            leftControlHBox.addChild(separator())

            leftControlHBox.addChild(Button("").apply {
                this.bounds.width.set(32f)
                this.skinID.set(EditorSkins.BUTTON)
                this.padding.set(Insets.ZERO)
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_results"]))
                this.setOnAction {
                    if (editor.allowedToEdit.get()) {
                        editorPane.openDialog(ResultsTextDialog(editorPane))
                    }
                }
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.results")))
            })
            leftControlHBox.addChild(Button("").apply {
                this.bounds.width.set(32f)
                this.skinID.set(EditorSkins.BUTTON)
                this.padding.set(Insets.ZERO)
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_world_settings"]))
                this.setOnAction {
                    val editor = editor
                    if (editor.allowedToEdit.get()) {
                        editor.attemptOpenGenericContextMenu(ContextMenu().also { ctxmenu ->
                            val currentWorldSettings = editor.world.worldSettings
                            val showInputIndicatorsVar = BooleanVar(currentWorldSettings.showInputIndicators).apply {
                                addListener {
                                    editor.world.worldSettings = editor.world.worldSettings.copy(showInputIndicators = it.getOrCompute())
                                }
                            }

                            ctxmenu.defaultWidth.set(350f)
                            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("editor.dialog.worldSettings.title"), editorPane.palette.markup))
                            ctxmenu.addMenuItem(SeparatorMenuItem())
                            ctxmenu.addMenuItem(CheckBoxMenuItem.create(showInputIndicatorsVar,
                                    Localization.getValue("editor.dialog.worldSettings.showInputIndicators"), editorPane.palette.markup))
                            
                            if (EditorSpecialFlags.STORY_MODE in editor.flags) {
                                ctxmenu.defaultWidth.set(450f)
                                ctxmenu.addMenuItem(SeparatorMenuItem())
                                ctxmenu.addMenuItem(LabelMenuItem.create("[b]Story Mode engine modifiers:[]", editorPane.palette.markup))

                                ctxmenu.addMenuItem(CustomMenuItem(HBox().also { hbox ->
                                    hbox.spacing.set(8f)
                                    hbox.bounds.height.set(32f)
                                    hbox += TextLabel("Lives:").apply {
                                        this.markup.set(editor.editorPane.palette.markup)
                                        this.renderAlign.set(Align.right)
                                        this.bounds.width.set(100f)
                                    }
                                    hbox += ComboBox((0..10).toList(), editor.container.storyModeMetadata.getOrCompute().lives).also { combobox ->
                                        combobox.markup.set(editor.editorPane.palette.markup)
                                        combobox.itemStringConverter.set {
                                            if (it == 0) "<disabled>" else "$it"
                                        }
                                        combobox.onItemSelected = {
                                            val old = editor.container.storyModeMetadata.getOrCompute()
                                            editor.container.storyModeMetadata.set(old.copy(lives = it))
                                        }
                                        
                                        combobox.bindWidthToParent(adjust = -108f)
                                    }
                                }))
                                ctxmenu.addMenuItem(CustomMenuItem(HBox().also { hbox ->
                                    hbox.spacing.set(8f)
                                    hbox.bounds.height.set(32f)
                                    hbox += TextLabel("Defective rod threshold:").apply {
                                        this.markup.set(editor.editorPane.palette.markup)
                                        this.renderAlign.set(Align.right)
                                        this.bounds.width.set(250f)
                                    }
                                    hbox += ComboBox((0..10).toList(), editor.container.storyModeMetadata.getOrCompute().defectiveRodsThreshold).also { combobox ->
                                        combobox.markup.set(editor.editorPane.palette.markup)
                                        combobox.itemStringConverter.set {
                                            if (it == 0) "<disabled>" else "$it"
                                        }
                                        combobox.onItemSelected = {
                                            val old = editor.container.storyModeMetadata.getOrCompute()
                                            editor.container.storyModeMetadata.set(old.copy(defectiveRodsThreshold = it))
                                        }
                                        
                                        combobox.bindWidthToParent(adjust = -258f)
                                    }
                                }))
                                ctxmenu.addMenuItem(CustomMenuItem(HBox().also { hbox ->
                                    hbox.spacing.set(8f)
                                    hbox.bounds.height.set(32f)
                                    hbox += TextLabel("Input restriction:").apply {
                                        this.markup.set(editor.editorPane.palette.markup)
                                        this.renderAlign.set(Align.right)
                                        this.bounds.width.set(200f)
                                    }
                                    hbox += ComboBox(InputTimingRestriction.VALUES, editor.container.storyModeMetadata.getOrCompute().inputTimingRestriction).also { combobox ->
                                        combobox.markup.set(editor.editorPane.palette.markup)
                                        combobox.onItemSelected = {
                                            val old = editor.container.storyModeMetadata.getOrCompute()
                                            editor.container.storyModeMetadata.set(old.copy(inputTimingRestriction = it))
                                            editor.container.resetInputFeedbackEntities()
                                        }
                                        
                                        combobox.bindWidthToParent(adjust = -208f)
                                    }
                                }))
                            }
                        })
                    }
                }
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.worldSettings")))
            })
            leftControlHBox.addChild(separator())
            leftControlHBox.addChild(Button("").apply {
                this.bounds.width.set(32f)
                this.skinID.set(EditorSkins.BUTTON)
                this.padding.set(Insets.ZERO)
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_music"]))
                this.setOnAction {
                    if (editor.allowedToEdit.get()) {
                        editorPane.openDialog(editorPane.musicDialog.prepareShow())
                    }
                }
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.music")))
            })
            leftControlHBox.addChild(IndentedButton("").apply {
                editorPane.styleIndentedButton(this)
                this.bounds.width.set(32f)
                this.skinID.set(EditorSkins.BUTTON)
                this.padding.set(Insets.ZERO)
                this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.metronome")))
                this.selectedState.addListener {
                    editor.metronomeEnabled.set(it.getOrCompute())
                }
                val inactive = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_metronome"])
                val active = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_metronome_active"])
                this += ImageNode(null).apply {
                    this.textureRegion.bind {
                        val isActive = selectedState.use()
                        if (isActive) active else inactive
                    }
                }
            })
            leftControlHBox.addChild(IndentedButton("").apply {
                editorPane.styleIndentedButton(this)
                this.bounds.width.set(32f)
                this.skinID.set(EditorSkins.BUTTON)
                this.padding.set(Insets.ZERO)
                val inactiveTooltip = Localization.getVar("editor.button.tapalong")
                val activeTooltip = Localization.getVar("editor.button.tapalong.active")
                this.tooltipElement.set(editorPane.createDefaultTooltip {
                    if (this@apply.selectedState.use()) activeTooltip.use() else inactiveTooltip.use()
                })
                tapalongPane.visible.set(this.selectedState.get())
                selectedState.addListener {
                    val state = it.getOrCompute()

                    if (state) {
                        editorPane.enqueueAnimation(tapalongPane.opacity, 0f, 1f, 0.125f).apply { 
                            onStart = {
                                tapalongPane.visible.set(true)
                            }
                        }
                    } else {
                        editorPane.enqueueAnimation(tapalongPane.opacity, 1f, 0f, 0.125f).apply {
                            onComplete = {
                                tapalongPane.visible.set(false)
                            }
                        }
                    }
                }
                this += ImageNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_tapalong"]))
            })
            leftControlHBox.addChild(tapalongPane)
        }
    }
    
    fun createPlaybackButtonSet(): Pane {
        val playbackButtonPane = HBox().apply {
            this.spacing.set(4f)
            this.bounds.width.set(32f * 3 + this.spacing.get() * 2)
            this.align.set(HBox.Align.CENTRE)
        }
        val pauseButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            val active: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_pause_color"])
            val inactive: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_pause_white"])
            this += ImageNode(null).apply {
                this.textureRegion.bind {
                    if (apparentDisabledState.use()) inactive else active
                }
                this.tint.bind {
                    if (apparentDisabledState.use()) Color.GRAY else Color.WHITE
                }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.pause")))
            this.disabled.bind { editorPane.editor.playState.use() != PlayState.PLAYING }
            this.setOnAction {
                editorPane.editor.changePlayState(PlayState.PAUSED)
            }
        }
        val playButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            val active: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_play_color"])
            val inactive: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_play_white"])
            this += ImageNode(null).apply {
                this.textureRegion.bind {
                    if (apparentDisabledState.use()) inactive else active
                }
                this.tint.bind {
                    if (apparentDisabledState.use()) Color.GRAY else Color.WHITE
                }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.play")))
            this.disabled.bind { editorPane.editor.playState.use() == PlayState.PLAYING }
            this.setOnAction {
                editorPane.editor.changePlayState(PlayState.PLAYING)
            }
        }
        val stopButton = Button("").apply {
            this.padding.set(Insets.ZERO)
            this.bounds.width.set(32f)
            this.skinID.set(EditorSkins.BUTTON)
            val active: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_stop_color"])
            val inactive: TextureRegion = TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["toolbar_stop_white"])
            this += ImageNode(null).apply {
                this.textureRegion.bind {
                    if (apparentDisabledState.use()) inactive else active
                }
                this.tint.bind {
                    if (apparentDisabledState.use()) Color.GRAY else Color.WHITE
                }
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.button.stop")))
            this.disabled.bind { editorPane.editor.playState.use() == PlayState.STOPPED }
            this.setOnAction {
                editorPane.editor.changePlayState(PlayState.STOPPED)
            }
        }
        playbackButtonPane.temporarilyDisableLayouts {
            playbackButtonPane += pauseButton
            playbackButtonPane += playButton
            playbackButtonPane += stopButton
        }
        return playbackButtonPane
    }

}