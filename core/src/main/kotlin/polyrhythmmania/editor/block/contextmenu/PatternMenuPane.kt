package polyrhythmmania.editor.block.contextmenu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.ImageNode
import io.github.chrislo27.paintbox.ui.ImageRenderingMode
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.border.SolidBorder
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.control.TextLabel
import polyrhythmmania.Localization
import polyrhythmmania.editor.block.CubeType
import polyrhythmmania.editor.block.PatternBlockData
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.util.RodinSpecialChars


class PatternMenuPane(val editorPane: EditorPane, val data: PatternBlockData)
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

                pane.addChild(Button(binding = { clearL10NStr.use() }, font = editorPane.main.mainFont).apply {
                    this.bounds.width.set(blockSize * 1.5f)
                    this.bounds.height.set(blockSize * 0.75f)
                    this.bounds.x.set((data.rowCount + 1.125f) * blockSize)
                    this.bounds.y.set(0 * blockSize + blockSize * 0.25f * 0.5f)
                    this.setOnAction {
                        buttons.forEach { it.cube.set(CubeType.NONE) }
                    }
                })
            }
        }

        addChild(createRowPane("${RodinSpecialChars.BORDERED_DPAD}:", data.rowDpadTypes, false))
        addChild(createRowPane("${RodinSpecialChars.BORDERED_A}:", data.rowATypes, true).also { pane ->
            pane.bounds.y.set(blockSize)
        })
        
        this.bounds.width.set(490f)
        this.bounds.height.set(80f)
    }

    inner class CubeButton(val isA: Boolean, cube: CubeType) : Button("") {

        val cube: Var<CubeType> = Var(cube)
        val image: ImageNode = ImageNode(null, ImageRenderingMode.MAINTAIN_ASPECT_RATIO)

        init {
            image.textureRegion.bind {
                val palette = editorPane.palette
                when (this@CubeButton.cube.use()) {
                    CubeType.NONE -> palette.blockFlatNoneRegion
                    CubeType.PLATFORM -> palette.blockFlatPlatformRegion
                    CubeType.PISTON -> if (isA) palette.blockFlatPistonARegion else palette.blockFlatPistonDpadRegion
                }
            }
            addChild(image)
            this.padding.set(Insets(2f))
            this.border.set(Insets(1f))
            this.borderStyle.set(SolidBorder(Color.BLACK))
//            this.skinID.set(ContextMenu.CONTEXT_MENU_BUTTON_SKIN_ID)
        }

        init {
            setOnAction {
                val values = CubeType.VALUES
                val current = this.cube.getOrCompute()
                val currentIndex = values.indexOf(current)
                this.cube.set(values[(currentIndex + 1) % values.size])
            }
        }
    }
}