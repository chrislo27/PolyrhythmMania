package polyrhythmmania.editor.block.contextmenu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.ImageNode
import io.github.chrislo27.paintbox.ui.ImageRenderingMode
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.border.SolidBorder
import io.github.chrislo27.paintbox.ui.contextmenu.ContextMenu
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.control.TextLabel
import polyrhythmmania.editor.block.CubeType
import polyrhythmmania.editor.block.PatternBlockData
import polyrhythmmania.editor.block.RowSetting
import polyrhythmmania.editor.pane.EditorPane
import polyrhythmmania.util.RodinSpecialChars


class PatternMenuPane(val editorPane: EditorPane, val data: PatternBlockData)
    : Pane() {

    init {
        val pane = Pane().also { pane ->
            Anchor.TopLeft.configure(this)
            val blockSize = 32f + 3f * 2
            
            pane.bounds.width.set(blockSize * (data.rowCount + 1))
            pane.bounds.height.set(blockSize * 2)
            
            pane.addChild(TextLabel("${RodinSpecialChars.BORDERED_DPAD}:", font = editorPane.palette.main.fontRodin).also { label ->
                label.bounds.x.set(0f)
                label.bounds.y.set(0 * blockSize)
                label.bounds.width.set(blockSize * 1)
                label.bounds.height.set(blockSize)
                label.padding.set(Insets(0f, 0f, 2f, 2f))
                label.margin.set(Insets(0f, 0f, 0f, 4f))
                label.renderAlign.set(Align.right)
                label.textAlign.set(TextAlign.RIGHT)
            })
            pane.addChild(TextLabel("${RodinSpecialChars.BORDERED_A}:", font = editorPane.palette.main.fontRodin).also { label ->
                label.bounds.x.set(0f)
                label.bounds.y.set(1 * blockSize)
                label.bounds.width.set(blockSize * 1)
                label.bounds.height.set(blockSize)
                label.padding.set(Insets(0f, 0f, 2f, 2f))
                label.margin.set(Insets(0f, 0f, 0f, 4f))
                label.renderAlign.set(Align.right)
                label.textAlign.set(TextAlign.RIGHT)
            })

            data.rowDpadTypes.forEachIndexed { index, cubeType ->
                pane.addChild(CubeButton(false, cubeType).also { button ->
                    button.cube.addListener {
                        val c = it.getOrCompute()
                        data.rowDpadTypes[index] = c
                    }
                    button.bounds.width.set(blockSize)
                    button.bounds.height.set(blockSize)
                    button.bounds.x.set((index + 1) * blockSize)
                    button.bounds.y.set(0 * blockSize)
                })
            }
            data.rowATypes.forEachIndexed { index, cubeType ->
                pane.addChild(CubeButton(true, cubeType).also { button ->
                    button.cube.addListener {
                        val c = it.getOrCompute()
                        data.rowATypes[index] = c
                    }
                    button.bounds.width.set(blockSize)
                    button.bounds.height.set(blockSize)
                    button.bounds.x.set((index + 1) * blockSize)
                    button.bounds.y.set(1 * blockSize)
                })
            }
        }
        
        
        addChild(pane)
        this.bounds.width.set(450f)
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