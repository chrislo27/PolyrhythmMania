package polyrhythmmania.editor.block.contextmenu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.ImageRenderingMode
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.Button
import paintbox.ui.control.ButtonSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.Palette
import polyrhythmmania.editor.block.CubeTypeLike
import polyrhythmmania.editor.block.data.AbstractPatternBlockData
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.util.RodinSpecialChars


abstract class AbstractPatternMenuPane<E : CubeTypeLike, Data : AbstractPatternBlockData<E>>(
        val editorPane: EditorPane, val data: Data, val clearType: E, beatIndexStart: Int
) : Pane() {
    
    companion object {
        fun invertButtonSkin(button: Button) {
            (button.skin.getOrCompute() as ButtonSkin).also { skin ->
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
        }
    }

    init {
        val blockSize = 32f + 3f * 2
        val clearL10NStr = Localization.getVar("blockContextMenu.spawnPattern.clear")

        fun createRowPane(label: String, rowTypes: MutableList<E>, isARow: Boolean): Pair<Pane, List<CubeButton>> {
            val cubeButtons: MutableList<CubeButton> = mutableListOf()
            return Pane().also { pane ->
                Anchor.TopLeft.configure(this)

                pane.bounds.width.set(blockSize * (data.rowCount + 3))
                pane.bounds.height.set(blockSize)

                pane.addChild(TextLabel(label, font = editorPane.palette.main.fontEditorRodin).also { label ->
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
                    cubeButtons += b
                    pane.addChild(b)
                }

                pane.addChild(Pane().apply {
                    this.bounds.x.set((data.rowCount + 1) * blockSize)
                    this.bounds.width.set(blockSize * 2)
                    this.addChild(Button(binding = { clearL10NStr.use() }, font = editorPane.main.fontEditor).apply {
                        Anchor.Centre.configure(this)
                        this.bounds.width.set(blockSize * 1.5f)
                        this.bounds.height.set(blockSize * 0.75f)
                        this.setOnAction {
                            cubeButtons.forEach { it.cube.set(clearType) }
                        }
                        invertButtonSkin(this)
                    })
                })
            } to cubeButtons
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
            vbox += Pane().apply {
                val dpadRowPane = createRowPane("${RodinSpecialChars.BORDERED_DPAD}:", data.rowDpadTypes, false)
                val aRowPane = createRowPane("${RodinSpecialChars.BORDERED_A}:", data.rowATypes, true)
                
                this += VBox().apply { 
                    this.spacing.set(0f)
                    this += dpadRowPane.first
                    this += aRowPane.first
                    this.sizeHeightToChildren(1f)
                    this.sizeWidthToChildren(1f)
                }
                this.sizeHeightToChildren(1f)
                
                this += Button(Localization.getVar("blockContextMenu.spawnPattern.swap"), font = editorPane.main.fontEditor).apply {
                    Anchor.CentreRight.configure(this)
                    this.bounds.width.set(blockSize * 1.5f)
                    this.bounds.height.set(blockSize * 0.75f)
                    this.setOnAction {
                        val dpadButtonStates = dpadRowPane.second.map { it.cube.getOrCompute() }
                        aRowPane.second.zip(dpadRowPane.second).forEachIndexed { index, (a, dpad) ->
                            dpad.cube.set(a.cube.getOrCompute())
                            a.cube.set(dpadButtonStates[index])
                        }
                    }
                    invertButtonSkin(this)
                }
            }
        }

        this.bounds.width.set(blockSize * (data.rowCount + 4.5f /* label + Clear + Swap */))
        this.bounds.height.set(90f)
    }
    
    protected abstract fun getTexRegForType(type: E, palette: Palette, isA: Boolean): TextureRegion

    inner class CubeButton(val isA: Boolean, cube: E) : Button("") {

        val cube: Var<E> = Var(cube)
        val image: ImageNode = ImageNode(null, ImageRenderingMode.MAINTAIN_ASPECT_RATIO)

        init {
            image.textureRegion.bind {
                val palette = editorPane.palette
                getTexRegForType(this@CubeButton.cube.use(), palette, isA)
            }
            addChild(image)
            this.padding.set(Insets(2f))
            this.border.set(Insets(1f))
            this.borderStyle.set(SolidBorder(Color.BLACK))
            image.tint.bind { (skin.use() as ButtonSkin).bgColorToUse.use() }
        }

        init {
            setOnAction {
                val values = data.allowedTypes
                val current = this.cube.getOrCompute()
                val currentIndex = values.indexOf(current)
                this.cube.set(values[(currentIndex + 1) % values.size])
            }
            setOnAltAction {
                val values = data.allowedTypes
                val current = this.cube.getOrCompute()
                val currentIndex = values.indexOf(current)
                this.cube.set(values[(currentIndex - 1 + values.size) % values.size])
            }
            this.tooltipElement.set(editorPane.createDefaultTooltip(binding = {
                Localization.getValue(this@CubeButton.cube.use().localizationNameKey)
            }))
        }
    }
}