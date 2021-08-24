package polyrhythmmania.editor.block.contextmenu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.Button
import paintbox.ui.control.ButtonSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.block.CubeType
import polyrhythmmania.editor.block.PatternBlockData
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.util.RodinSpecialChars


class PatternMenuPane(val editorPane: EditorPane, val data: PatternBlockData, val clearType: CubeType, beatIndexStart: Int)
    : Pane() {

    init {
        val blockSize = 32f + 3f * 2
        val clearL10NStr = Localization.getVar("blockContextMenu.spawnPattern.clear")

        fun createRowPane(label: String, rowTypes: Array<CubeType>, isARow: Boolean): Pane {
            return Pane().also { pane ->
                Anchor.TopLeft.configure(this)

                pane.bounds.width.set(blockSize * (data.rowCount + 3))
                pane.bounds.height.set(blockSize)

                pane.addChild(TextLabel(label, font = editorPane.palette.main.fontRodinFixed).also { label ->
                    label.bounds.x.set(0f)
                    label.bounds.y.set(0 * blockSize)
                    label.bounds.width.set(blockSize * 1)
                    label.bounds.height.set(blockSize)
                    label.padding.set(Insets(0f, 0f, 2f, 2f))
                    label.margin.set(Insets(0f, 0f, 0f, 4f))
                    label.renderAlign.set(Align.right)
                    label.textAlign.set(TextAlign.RIGHT)
                    label.textColor.set(Color.BLACK)
                    label.tooltipElement.set(editorPane.createDefaultTooltip(Localization.getVar("blockContextMenu.spawnPattern.cycleHint")))
                })

                val buttons: MutableList<CubeButton> = mutableListOf()
                rowTypes.forEachIndexed { index, cubeType ->
                    val b = CubeButton(isARow, cubeType).also { button ->
                        button.cube.addListener {
                            val c = it.getOrCompute()
                            rowTypes[index] = c
                        }
                        button.bounds.width.set(blockSize)
                        button.bounds.height.set(blockSize)
                        button.bounds.x.set((index + 1) * blockSize)
                        button.bounds.y.set(0 * blockSize)
                    }
                    buttons += b
                    pane.addChild(b)
                }

                pane.addChild(Pane().apply {
                    this.bounds.x.set((data.rowCount + 1) * blockSize)
                    this.bounds.width.set(blockSize * 2)
                    this.addChild(Button(binding = { clearL10NStr.use() }, font = editorPane.main.mainFont).apply {
                        Anchor.Centre.configure(this)
                        this.bounds.width.set(blockSize * 1.5f)
                        this.bounds.height.set(blockSize * 0.75f)
                        this.setOnAction {
                            buttons.forEach { it.cube.set(clearType) }
                        }
                        (this.skin.getOrCompute() as ButtonSkin).also { skin ->
                            listOf(skin.defaultBgColor, skin.disabledBgColor, skin.hoveredBgColor,
                                    skin.pressedAndHoveredBgColor, skin.pressedBgColor,
                                    skin.defaultTextColor, skin.disabledTextColor, skin.hoveredTextColor,
                                    skin.pressedAndHoveredTextColor, skin.pressedTextColor).forEach {
                                val color = it.getOrCompute()
                                color.r = 1f - color.r
                                color.g = 1f - color.g
                                color.b = 1f - color.b
                            }
                        }
                    })
                })
            }
        }
        
        val beatLabels = Pane().also { pane ->
            pane.bounds.width.set(blockSize * (data.rowCount + 3))
            pane.bounds.height.set(10f)
            
            for (b in 0 until (data.rowCount / 2)) {
                pane += TextLabel("${b + beatIndexStart}", font = editorPane.palette.musicDialogFontBold).apply { 
                    this.bounds.width.set(blockSize)
                    this.bounds.x.set((b * 2 + 1) * blockSize)
                    this.textAlign.set(TextAlign.LEFT)
                    this.renderAlign.set(Align.bottomLeft)
                    this.textColor.set(Color.BLACK)
                    this.padding.set(Insets(0f, 2f, 1f, 1f))
                    this.setScaleXY(0.75f)
                }
            }
        }

        val vbox = VBox().apply { 
            this.spacing.set(0f)
        }
        this += vbox
        vbox.temporarilyDisableLayouts { 
            vbox += beatLabels
            vbox += createRowPane("${RodinSpecialChars.BORDERED_DPAD}:", data.rowDpadTypes, false)
            vbox += createRowPane("${RodinSpecialChars.BORDERED_A}:", data.rowATypes, true)
        }

        this.bounds.width.set(blockSize * (data.rowCount + 3 /* label + Clear */))
        this.bounds.height.set(90f)
    }

    inner class CubeButton(val isA: Boolean, cube: CubeType) : Button("") {

        val cube: Var<CubeType> = Var(cube)
        val image: ImageNode = ImageNode(null, ImageRenderingMode.MAINTAIN_ASPECT_RATIO)

        init {
            image.textureRegion.bind {
                val palette = editorPane.palette
                when (this@CubeButton.cube.use()) {
                    CubeType.NONE -> palette.blockFlatNoneRegion
                    CubeType.NO_CHANGE -> palette.blockFlatNoChangeRegion
                    CubeType.PLATFORM -> palette.blockFlatPlatformRegion
                    CubeType.PISTON -> if (isA) palette.blockFlatPistonARegion else palette.blockFlatPistonDpadRegion
                    CubeType.PISTON_OPEN -> if (isA) palette.blockFlatPistonAOpenRegion else palette.blockFlatPistonDpadOpenRegion
                }
            }
            addChild(image)
            this.padding.set(Insets(2f))
            this.border.set(Insets(1f))
            this.borderStyle.set(SolidBorder(Color.BLACK))
            image.tint.bind { (skin.use() as ButtonSkin).bgColorToUse.use() }
//            this.skinID.set(ContextMenu.CONTEXT_MENU_BUTTON_SKIN_ID)
        }

        init {
            setOnAction {
                val values = data.allowedCubeTypes
                val current = this.cube.getOrCompute()
                val currentIndex = values.indexOf(current)
                this.cube.set(values[(currentIndex + 1) % values.size])
            }
            setOnAltAction {
                val values = data.allowedCubeTypes
                val current = this.cube.getOrCompute()
                val currentIndex = values.indexOf(current)
                this.cube.set(values[(currentIndex - 1 + values.size) % values.size])
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(binding = {
                Localization.getValue(this@CubeButton.cube.use().localizationNameKey) // Could be getVar but not necessary in a context menu
            }))
        }
    }
}