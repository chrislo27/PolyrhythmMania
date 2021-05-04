package polyrhythmmania.editor.pane

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.control.ButtonSkin
import io.github.chrislo27.paintbox.ui.element.RectElement
import io.github.chrislo27.paintbox.util.gdxutils.drawCompressed
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.track.block.Instantiator
import polyrhythmmania.editor.track.block.Instantiators
import kotlin.math.roundToInt


class InstantiatorPane(val upperPane: UpperPane) : Pane() {

    val editorPane: EditorPane = upperPane.editorPane
    val editor: Editor = upperPane.editor

    val list: InstantiatorList

    init {
        val middleDivider = RectElement(binding = { editorPane.palette.instantiatorPaneBorder.use() }).apply {
            Anchor.TopCentre.configure(this)
            this.margin.set(Insets(6f))
            this.bounds.width.bind {
                val margin = margin.use()
                (2f + margin.left + margin.right).roundToInt().toFloat()
            }
            this.bounds.x.set(300f)
        }
        this += middleDivider

        val scrollSelector = Pane().apply {
            this.bounds.width.bind { middleDivider.bounds.x.use() }
        }
        this += scrollSelector

        val descPane = Pane().apply {
            Anchor.TopRight.configure(this)
            this.bounds.width.bind {
                (parent.use()?.contentZone?.width?.use()
                        ?: 0f) - (middleDivider.bounds.x.use() + middleDivider.bounds.width.use())
            }
        }
        this += descPane

        list = InstantiatorList(this).apply {

        }
        scrollSelector += list
    }

    init {
    }

}

class InstantiatorList(val instantiatorPane: InstantiatorPane) : Pane() {

    val upperPane: UpperPane = instantiatorPane.upperPane
    val editorPane: EditorPane = instantiatorPane.editorPane
    val editor: Editor = instantiatorPane.editor

    private val list: List<Instantiator> = Instantiators.list
    val buttonPane: Pane
    val listPane: Pane
    val listView: ListView

    private var index: Var<Int> = Var(0)

    init {
        val buttonWidth = 32f

        buttonPane = Pane().apply {
            this.bounds.width.set(buttonWidth + 2f)
            this.margin.set(Insets(0f, 0f, 0f, 2f))
        }
        this += buttonPane
        buttonPane += Button("").apply {
            this.padding.set(Insets.ZERO)
            Anchor.TopLeft.configure(this)
            this.bounds.width.set(buttonWidth)
            this.bounds.height.set(buttonWidth)
            this.skinID.set(EditorSkins.BUTTON_NO_SKIN)
            this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_icon_buttons_editor"), 16 * 3, 16 * 3, 16, 16)).apply {
                this.tint.bind { (skin.use() as ButtonSkin).bgColorToUse.use() }
            }
            this.setOnAction {
                scroll(-1)
            }
            this.disabled.bind { 
                index.use() <= 0
            }
        }
        buttonPane += Button("").apply {
            this.padding.set(Insets.ZERO)
            Anchor.BottomLeft.configure(this)
            this.bounds.width.set(buttonWidth)
            this.bounds.height.set(buttonWidth)
            this.skinID.set(EditorSkins.BUTTON_NO_SKIN)
            this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_icon_buttons_editor"), 16 * 3, 16 * 3, 16, 16)).apply {
                this.tint.bind { (skin.use() as ButtonSkin).bgColorToUse.use() }
                rotation.set(180f)
            }
            this.setOnAction {
                scroll(+1)
            }
            this.disabled.bind {
                index.use() >= list.size - 1
            }
        }
        buttonPane += ImageNode(TextureRegion(AssetRegistry.get<Texture>("ui_icon_buttons_editor"), 16 * 6, 16 * 4, 16, 16)).apply {
            this.padding.set(Insets.ZERO)
            Anchor.CentreLeft.configure(this)
            this.bounds.width.set(buttonWidth)
            this.bounds.height.set(buttonWidth)
        }

        listPane = Pane().apply {
            bounds.x.set(buttonWidth + 2f)
            bindWidthToParent(-(buttonWidth + 2f))
        }
        this += listPane
        listView = this.ListView()
        listPane += listView
    }

    init {
        listView.addInputEventListener { event ->
            when (event) {
                is ClickPressed -> {
                    if (event.button == Input.Buttons.LEFT) {
                        editor.attemptInstantiatorDrag(this.getCurrentInstantiator())
                        true
                    } else false
                }
                else -> false
            }
        }
        addInputEventListener { event ->
            when (event) {
                is Scrolled -> {
                    scroll(event.amountY.roundToInt())
                    true
                }
                else -> false
            }
        }
    }

    fun scroll(down: Int) {
        if (down == 0) return
        val future = (index.getOrCompute() + down).coerceIn(list.indices)
        index.set(future)
    }

    fun getCurrentInstantiator(): Instantiator {
        return list[index.getOrCompute()]
    }

    inner class ListView : Pane() {
        private val instantiatorList: InstantiatorList = this@InstantiatorList

        private var indexTween: Float = instantiatorList.index.getOrCompute().toFloat()

        init {
            this.doClipping.set(true)
        }

        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val currentIndex = instantiatorList.index.getOrCompute()
            val indexAsFloat = currentIndex.toFloat()
            if (indexTween != indexAsFloat) {
                indexTween = MathUtils.lerp(indexTween, indexAsFloat, (Gdx.graphics.deltaTime / 0.075f).coerceIn(0f, 1f))
                if (MathUtils.isEqual(indexTween, indexAsFloat, 0.005f)) {
                    indexTween = indexAsFloat
                }
            }

            val renderBounds = this.contentZone
            val x = renderBounds.x.getOrCompute() + originX
            val y = originY - renderBounds.y.getOrCompute()
            val w = renderBounds.width.getOrCompute()
            val h = renderBounds.height.getOrCompute()
            val lastPackedColor = batch.packedColor
            val opacity = apparentOpacity.getOrCompute()

            val paintboxFont = editorPane.palette.instantiatorFont
            val instantiators = instantiatorList.list

            paintboxFont.useFont { font ->
                val capHeight = font.capHeight
                val lineHeight = font.lineHeight * 1.5f
                val yOffset = -(h * 0.5f) + capHeight * 0.5f + (indexTween * lineHeight)
                instantiators.forEachIndexed { index, instantiator ->
                    if (index == currentIndex) {
                        font.setColor(0.65f, 1f, 1f, opacity)
                    } else {
                        font.setColor(1f, 1f, 1f, opacity)
                    }
                    font.drawCompressed(batch, instantiator.name.getOrCompute(), x, y - index * lineHeight + yOffset, w, Align.left)
                }
            }

            batch.packedColor = lastPackedColor
        }
    }
}