package polyrhythmmania.editor.pane.dialog

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.RenderAlign
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.ColumnarHBox
import paintbox.ui.layout.ColumnarVBox
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.block.data.SpotlightsColorData
import polyrhythmmania.editor.block.data.SwitchedLightColor
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.editor.pane.IndentedButton
import polyrhythmmania.ui.ColourPicker
import polyrhythmmania.util.RodinSpecialChars
import polyrhythmmania.world.spotlights.Spotlights
import kotlin.math.roundToInt


class SpotlightEditDialog(
        editorPane: EditorPane,
        val colorData: SpotlightsColorData,
) : EditorDialog(editorPane) {
    
    private enum class CopyProperties(val localizationKey: String) {
        ALL("editor.dialog.spotlightsAdvanced.copy.properties.all"),
        COLOR("editor.dialog.spotlightsAdvanced.copy.properties.color"),
        STRENGTH("editor.dialog.spotlightsAdvanced.copy.properties.strength"),
        TRIGGER("editor.dialog.spotlightsAdvanced.copy.properties.trigger"),
    }

    data class LightSelection(val lightColor: SwitchedLightColor, val rowIndex: Int, val indexInRow: Int) {
        fun isGlobal(): Boolean = rowIndex == -1
    }
    
    val ambientSelection: LightSelection = LightSelection(colorData.ambientLight, -1, -1)
    val allRows: List<List<LightSelection>> = colorData.rows.mapIndexed { rowIndex, list -> 
        list.mapIndexed { index, it -> LightSelection(it, rowIndex, index) }
    }
    val rowASelection: List<LightSelection> = allRows[0]
    val rowDpadSelection: List<LightSelection> = allRows[1]
    val allSelections: List<LightSelection> = listOf(ambientSelection) + allRows.flatten()
    val selection: Var<LightSelection> = Var(ambientSelection)
    
    private var blockUIElementsFromUpdating: Boolean = false
    val colourPicker: ColourPicker = ColourPicker(hasAlpha = true, font = editorPane.palette.musicDialogFont).apply {
        this.showAlphaPane.set(true)
    }
    val strengthSlider: Slider = Slider().apply { 
        this.minimum.set(0f)
        this.maximum.set(100f)
        this.tickUnit.set(5f)
    }
    val enabledCheckbox: CheckBox = CheckBox(Localization.getVar("editor.dialog.spotlightsAdvanced.enabled"), font = editorPane.palette.musicDialogFont).apply { 
        this.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("editor.dialog.spotlightsAdvanced.enabled.tooltip")))
    }
    
    private val genericUpdateTrigger: BooleanVar = BooleanVar(false)
    private val inCopyOp: BooleanVar = BooleanVar(false)
    private val copyTargets: Var<Set<LightSelection>> = Var(emptySet())

    init {
        val palette = editorPane.palette
        this.titleLabel.text.bind { Localization.getVar("editor.dialog.spotlightsAdvanced.title").use() }

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
        
        val vbox = VBox().apply { 
            this.spacing.set(2f)
        }
        contentPane += vbox
        
        vbox += Pane().apply { 
            this.bindHeightToParent(multiplier = 0.275f)

            this += TextLabel(binding = {
                if (inCopyOp.use()) 
                    Localization.getVar("editor.dialog.spotlightsAdvanced.copy.lightSourceTitle").use()
                else Localization.getVar("editor.dialog.spotlightsAdvanced.selectLight").use()
            }, palette.musicDialogFontBold).apply {
                Anchor.TopLeft.configure(this)
                this.textColor.set(Color.WHITE)
                this.renderAlign.set(RenderAlign.left)
                this.bounds.width.set(390f)
                this.bounds.height.set(32f)
            }
            this += HBox().apply { 
                Anchor.BottomCentre.configure(this)
                this.bindHeightToParent(adjust = -32f)
                this.margin.set(Insets(8f))
                this.spacing.set(16f)
                this.align.set(HBox.Align.CENTRE)
                
                this += addIndentedButton(ambientSelection, Localization.getValue("editor.dialog.spotlightsAdvanced.light.ambient")).apply {
                    this.bindWidthToSelfHeight(multiplier = 2f)
                }
                this += ColumnarHBox(2, useRows = true).apply { 
                    this.spacing.set(2f)
                    this.bindWidthToSelfHeight(multiplier = (1 + Spotlights.NUM_ON_ROW) / 2f, adjust = this.spacing.get() * Spotlights.NUM_ON_ROW)

                    this += TextLabel(Localization.getVar("editor.dialog.spotlightsAdvanced.light.spotlight"), palette.musicDialogFont).apply {
                        Anchor.TopLeft.configure(this, offsetY = -26f)
                        this.textColor.set(Color.WHITE)
                        this.renderAlign.set(RenderAlign.topLeft)
                        this.bounds.width.set(200f)
                        this.bounds.height.set(26f)
                    }
                    
                    this.columnBoxes.forEachIndexed { rowIndex, box -> 
                        box.spacing.set(2f)
                        box.align.set(HBox.Align.LEFT)

                        box += TextLabel("${if (rowIndex == 0) RodinSpecialChars.BORDERED_DPAD else RodinSpecialChars.BORDERED_A}:", font = editorPane.palette.main.fontEditorRodin).also { label ->
                            label.bindWidthToSelfHeight()
                            label.padding.set(Insets(2f, 0f, 2f, 4f))
                            label.renderAlign.set(Align.right)
                            label.textAlign.set(TextAlign.RIGHT)
                            label.textColor.set(Color.WHITE)
                        }
                        
                        allRows[allRows.size - rowIndex - 1].forEachIndexed { i, sel ->
                            box += addIndentedButton(sel, Localization.getValue("editor.dialog.spotlightsAdvanced.light.indexed", i))
                        }
                    }
                }
            }
        }
        val spacerColor = Color(1f, 1f, 1f, 0.75f)
        vbox += RectElement(spacerColor).apply { 
            this.bounds.height.set(8f)
            this.margin.set(Insets(3f, 3f, 0f, 0f))
        }
        vbox += Pane().apply {
            this.bindHeightToParent(multiplier = 0.6f, adjust = 40f)
            this += TextLabel(Localization.getVar("editor.dialog.spotlightsAdvanced.adjustLightSettings"), palette.musicDialogFontBold).apply {
                Anchor.TopLeft.configure(this)
                this.textColor.set(Color.WHITE)
                this.renderAlign.set(RenderAlign.left)
                this.bounds.width.set(300f)
                this.bounds.height.set(32f)
                this.visible.bind { !inCopyOp.use() }
            }
            this += ColumnarVBox(listOf(6, 4), useRows = false).apply {
                this.bindHeightToParent(adjust = -32f)
                this.bounds.y.set(32f)
                this.spacing.set(20f)
                this.visible.bind { !inCopyOp.use() }
                this.setAllSpacers {
                    RectElement(spacerColor).apply {
                        this.margin.set(Insets(0f, 4f, 9f, 9f))
                    }
                }

                val left = this[0]
                left.align.set(VBox.Align.CENTRE)
                left.spacing.set(5f)
                left += colourPicker.apply {
                    this.bounds.height.set(220f)
                }
                left += HBox().apply {
                    this.spacing.set(12f)
                    this.align.set(HBox.Align.CENTRE)
                    this.bounds.height.set(40f)
                    this += Button(Localization.getValue("editor.dialog.spotlightsAdvanced.resetColor"), font = palette.musicDialogFont).apply {
                        this.bounds.width.set(300f)
                        this.applyDialogStyleContent()
                        this.setOnAction {
                            val sel = selection.getOrCompute()
                            sel.lightColor.color.set(sel.lightColor.resetColor)
                            setUIElementsFromLightColor()
                            genericUpdateTrigger.invert()
                        }
                    }
                }

                val right = this[1]
                right.align.set(VBox.Align.TOP)
                right.spacing.set(8f)
                right += HBox().apply {
                    this.spacing.set(6f)
                    this.bounds.height.set(40f)
                    this += TextLabel(Localization.getVar("editor.dialog.spotlightsAdvanced.strength", Var {
                        listOf(strengthSlider.value.use().roundToInt())
                    }), palette.musicDialogFont).apply {
                        Anchor.TopLeft.configure(this)
                        this.bindWidthToParent(multiplier = 0.5f, adjust = -3f)
                        this.textColor.set(Color.WHITE)
                        this.renderAlign.set(RenderAlign.left)
                    }
                    this += Button(Localization.getValue("editor.dialog.spotlightsAdvanced.resetStrength"), font = palette.musicDialogFont).apply {
                        this.bindWidthToParent(multiplier = 0.5f, adjust = -3f)
                        this.applyDialogStyleContent()
                        this.setOnAction {
                            val sel = selection.getOrCompute()
                            sel.lightColor.strength = sel.lightColor.defaultStrength
                            setUIElementsFromLightColor()
                            genericUpdateTrigger.invert()
                        }
                    }
                }
                right += strengthSlider.apply {
                    this.bounds.height.set(32f)
                }
                right += RectElement(spacerColor).apply {
                    this.bounds.height.set(8f)
                    this.margin.set(Insets(3f, 3f, 0f, 0f))
                }
                right += enabledCheckbox.apply {
                    this.bounds.height.set(32f)
                    this.color.set(Color.WHITE.cpy())
                }
                right += RectElement(spacerColor).apply {
                    this.bounds.height.set(8f)
                    this.margin.set(Insets(3f, 3f, 0f, 0f))
                }
            }
            
            this += VBox().apply {
                this.spacing.set(10f)
                this.visible.bind { inCopyOp.use() } 
                this.align.set(VBox.Align.CENTRE)
                
                this += TextLabel(Localization.getVar("editor.dialog.spotlightsAdvanced.copy.multiSelect"), palette.musicDialogFont).apply {
                    this.renderAlign.set(RenderAlign.center)
                    this.bounds.height.set(72f)
                    this.textColor.set(Color.WHITE)
                }
                this += HBox().apply { 
                    this.spacing.set(16f)
                    this.bounds.height.set(40f)
                    this.align.set(HBox.Align.CENTRE)
                    this += Button(Localization.getVar("editor.dialog.spotlightsAdvanced.copy.select.clear")).apply {
                        this.markup.set(palette.markup)
                        this.bounds.width.set(200f)
                        this.applyDialogStyleContent()
                        this.setOnAction {
                            copyTargets.set(emptySet())
                        }
                    }
                    this += Button(Localization.getVar("editor.dialog.spotlightsAdvanced.copy.select.all")).apply {
                        this.markup.set(palette.markup)
                        this.bounds.width.set(200f)
                        this.applyDialogStyleContent()
                        this.setOnAction {
                            copyTargets.set(allSelections.toSet() - selection.getOrCompute())
                        }
                    }
                    fun addRowButton(rowTxt: String, row: List<LightSelection>) {
                        this += Button(binding = {
                            (if (copyTargets.use().containsAll(row - selection.use())) {
                                Localization.getVar("editor.dialog.spotlightsAdvanced.copy.deselect.$rowTxt")
                            } else Localization.getVar("editor.dialog.spotlightsAdvanced.copy.select.$rowTxt")).use()
                        }).apply {
                            this.markup.set(palette.markup)
                            this.bounds.width.set(200f)
                            this.applyDialogStyleContent()
                            this.setOnAction {
                                val targets = copyTargets.getOrCompute()
                                val rowAsSet = row.toSet() - selection.getOrCompute()
                                if (targets.containsAll(rowAsSet)) {
                                    copyTargets.set(targets - rowAsSet)
                                } else copyTargets.set(targets + rowAsSet)
                            }
                        }
                    }
                    addRowButton("a", rowASelection)
                    addRowButton("dpad", rowDpadSelection)
                }
                this += RectElement(spacerColor).apply {
                    Anchor.TopCentre.configure(this)
                    this.bounds.height.set(8f)
                    this.bindWidthToParent(multiplier = 0.75f)
                    this.margin.set(Insets(3f, 3f, 0f, 0f))
                }
                val copyProperties = Var(CopyProperties.ALL)
                this += HBox().apply {
                    this.spacing.set(8f)
                    this.bounds.height.set(40f)
                    this.align.set(HBox.Align.CENTRE)
                    this += TextLabel(Localization.getVar("editor.dialog.spotlightsAdvanced.copy.properties"), palette.musicDialogFont).apply {
                        this.textColor.set(Color.WHITE)
                        this.margin.set(Insets(0f, 0f, 0f, 4f))
                        this.autosizeBehavior.set(TextLabel.AutosizeBehavior.Active(TextLabel.AutosizeBehavior.Dimensions.WIDTH_ONLY))
                    }
                    this += ComboBox(CopyProperties.values().toList(), copyProperties.getOrCompute(), font = palette.musicDialogFont).apply { 
                        this.bounds.width.set(220f)
                        this.itemStringConverter.set { Localization.getValue(it.localizationKey) }
                        this.onItemSelected = {
                            copyProperties.set(it)
                        }
                    }
                }
                this += RectElement(spacerColor).apply {
                    Anchor.TopCentre.configure(this)
                    this.bounds.height.set(8f)
                    this.bindWidthToParent(multiplier = 0.75f)
                    this.margin.set(Insets(3f, 3f, 0f, 0f))
                }
                this += Button(Localization.getVar("editor.dialog.spotlightsAdvanced.copy.paste", Var { 
                    listOf(copyTargets.use().size)
                }), font = palette.musicDialogFont).apply {
                    Anchor.TopCentre.configure(this)
                    this.bounds.width.set(450f)
                    this.bounds.height.set(50f)
                    this.applyDialogStyleContent()
                    this.disabled.bind {
                        copyTargets.use().isEmpty()
                    }
                    this.setOnAction {
                        val sel = selection.getOrCompute().lightColor
                        val targets = copyTargets.getOrCompute().map { it.lightColor } - sel
                        val props = copyProperties.getOrCompute()
                        targets.forEach { target ->
                            if (props == CopyProperties.ALL || props == CopyProperties.COLOR) {
                                target.color.set(sel.color)
                            }
                            if (props == CopyProperties.ALL || props == CopyProperties.STRENGTH) {
                                target.strength = sel.strength
                            }
                            if (props == CopyProperties.ALL || props == CopyProperties.TRIGGER) {
                                target.enabled = sel.enabled
                            }
                        }
                        
                        inCopyOp.set(false)
                        copyTargets.set(emptySet())
                        genericUpdateTrigger.invert()
                    }
                }
            }
        }
        
        bottomPane += HBox().apply {
            Anchor.TopCentre.configure(this)
            this.spacing.set(8f)
            this.align.set(HBox.Align.CENTRE)
            this.bindWidthToParent(multiplier = 0.6f)
            
            this += Button(binding = {
                if (inCopyOp.use()) {
                    Localization.getVar("editor.dialog.spotlightsAdvanced.copy.cancel").use()
                } else Localization.getVar("editor.dialog.spotlightsAdvanced.copy").use()
            }).apply {
                this.markup.set(editorPane.palette.markup)
                this.bounds.width.set(400f)
                this.applyDialogStyleBottom()
                this.setOnAction {
                    if (!inCopyOp.get()) {
                        inCopyOp.set(true)
                    } else {
                        inCopyOp.set(false)
                    }
                    copyTargets.set(emptySet())
                }
            }
        }
    }
    
    init {
        setUIElementsFromLightColor()
        genericUpdateTrigger.invert()
        
        selection.addListener {
            setUIElementsFromLightColor()
        }
        
        colourPicker.currentColor.addListener { c ->
            if (!blockUIElementsFromUpdating) {
                selection.getOrCompute().lightColor.color.set(c.getOrCompute().cpy())
                genericUpdateTrigger.invert()
            }
        }
        strengthSlider.value.addListener {
            if (!blockUIElementsFromUpdating) {
                selection.getOrCompute().lightColor.strength = it.getOrCompute() / 100f
                genericUpdateTrigger.invert()
            } 
        }
        enabledCheckbox.onCheckChanged = {
            if (!blockUIElementsFromUpdating) {
                selection.getOrCompute().lightColor.enabled = it
                genericUpdateTrigger.invert()
            }
        }
    }
    
    fun setUIElementsFromLightColor() {
        blockUIElementsFromUpdating = true
        
        val sel = selection.getOrCompute()
        colourPicker.setColor(sel.lightColor.color, true)
        strengthSlider.setValue(sel.lightColor.strength * 100)
        enabledCheckbox.checkedState.set(sel.lightColor.enabled)
        
        blockUIElementsFromUpdating = false
    }

    override fun canCloseDialog(): Boolean {
        return true
    }

    override fun onCloseDialog() {
        super.onCloseDialog()
        editor.updatePaletteAndTexPackChangesState()
    }
    
    private fun addIndentedButton(target: LightSelection, text: String): IndentedButton {
        val palette = editorPane.palette

        return IndentedButton(text, font = main.fontEditorBoldBordered).apply {
            this.bindWidthToSelfHeight()

            editorPane.styleIndentedButton(this)

            this.skinFactory.set { _ ->
                LightSelectionButtonSkin(target, this)
            }

            this.padding.set(Insets.ZERO)
            this.border.set(Insets(1f))
            this.borderStyle.set(SolidBorder(Color.WHITE))

            this.indentedButtonBorder.set(Insets(4f))
            this.indentedButtonBorderColor.bind {
                val sel = selection.use()
                if (sel == target) {
                    palette.toolbarIndentedButtonBorderTint.use()
                } else {
                    Color.BLUE
                }
            }

            this.selectedState.eagerBind {
                val sel = selection.use()
                val isCopying = inCopyOp.use()
                if (isCopying) {
                    sel == target || target in copyTargets.use()
                } else (sel == target)
            }

            this.disabled.eagerBind {
                val sel = selection.use()
                inCopyOp.use() && sel == target
            }

            this.setOnAction {
                if (inCopyOp.get()) {
                    val targets = copyTargets.getOrCompute()
                    if (target in targets) {
                        copyTargets.set(targets - target)
                    } else {
                        copyTargets.set(targets + target)
                    }
                } else {
                    selection.set(target)
                }
            }
        }
    }

    private inner class LightSelectionButtonSkin(
        target: LightSelection,
        element: IndentedButton,
    ) : ButtonSkin(element) {

        init {
            val skin = this
            skin.roundedRadius.set(0)
            listOf(
                skin.defaultTextColor,
                skin.disabledTextColor,
                skin.hoveredTextColor,
                skin.pressedAndHoveredTextColor,
                skin.pressedTextColor
            ).forEach {
                it.set(it.getOrCompute().cpy().apply {
                    r = 1f - r
                    g = 1f - g
                    b = 1f - b
                })
            }
            skin.defaultBgColor.sideEffectingAndRetain { color ->
                genericUpdateTrigger.use()
                color.set(target.lightColor.color)
                color
            }
            skin.defaultTextColor.sideEffectingAndRetain { c ->
                genericUpdateTrigger.use()
                if (target.lightColor.enabled) {
                    c.set(Color.WHITE).lerp(Color.YELLOW, target.lightColor.strength)
                } else {
                    c.set(Color.GRAY)
                }
                c
            }
            skin.disabledTextColor.bind {
                element.indentedButtonBorderColor.use()
            }
        }
    }
}
