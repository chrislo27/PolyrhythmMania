package paintbox.tests.newui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.Align
import paintbox.PaintboxScreen
import paintbox.font.TextAlign
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.*

internal class UITextLabelAlignTestScreen(override val main: NewUITestGame) : PaintboxScreen() {

    private val camera: OrthographicCamera = OrthographicCamera()
    private var root: SceneRoot = SceneRoot(camera)
        private set(value) {
            main.inputMultiplexer.removeProcessor(field.inputSystem)
            field = value
            main.inputMultiplexer.addProcessor(value.inputSystem)
        }

    init {
        populate()
    }

    private fun populate() {
        root = SceneRoot(camera)
        val bg = TestColorElement(Color(1f, 165f / 255f, 0.5f, 1f))
        root += bg
        
        fun createLabel(indexX: Int, indexY: Int, text: String, align: Int, textAlign: TextAlign, xComp: Boolean): TextLabel {
            return TextLabel(text).apply {
                this.border.set(Insets(1f))
                this.borderStyle.set(SolidBorder(Color.BLACK))
                this.bgPadding.set(Insets(6f))
                this.renderBackground.set(true)
                this.doXCompression.set(xComp)
                this.renderAlign.set(align)
                this.textAlign.set(textAlign)
                this.setScaleXY(0.9f)

                Anchor.TopLeft.configure(this, offsetX = 32f + 320f * indexY, offsetY = 50f + 40f * indexX)
                this.bounds.width.set(300f)
                this.bounds.height.set(40f)
            }
        }
        
        // Text Align: Left
        bg += createLabel(0, 0, "NoCmpr | Render: L | TA: L", Align.left, TextAlign.LEFT, false)
        bg += createLabel(1, 0, "XCmpr | Render: L | TA: L", Align.left, TextAlign.LEFT, true)
        bg += createLabel(2, 0, "Multi\nLine | Render: L | TA: L", Align.left, TextAlign.LEFT, true)
        bg += createLabel(3, 0, "Do X Compression Enabled | Render: L | TA: L", Align.left, TextAlign.LEFT, true)
        
        bg += createLabel(5, 0, "NoCmpr | Render: C | TA: L", Align.center, TextAlign.LEFT, false)
        bg += createLabel(6, 0, "XCmpr | Render: C | TA: L", Align.center, TextAlign.LEFT, true)
        bg += createLabel(7, 0, "Multi\nLine | Render: C | TA: L", Align.center, TextAlign.LEFT, true)
        bg += createLabel(8, 0, "Do X Compression Enabled | Render: C | TA: L", Align.center, TextAlign.LEFT, true)
        
        bg += createLabel(10, 0, "NoCmpr | Render: R | TA: L", Align.right, TextAlign.LEFT, false)
        bg += createLabel(11, 0, "XCmpr | Render: R | TA: L", Align.right, TextAlign.LEFT, true)
        bg += createLabel(12, 0, "Multi\nLine | Render: R | TA: L", Align.right, TextAlign.LEFT, true)
        bg += createLabel(13, 0, "Do X Compression Enabled | Render: R | TA: L", Align.right, TextAlign.LEFT, true)
        
        // Text Align: Centre
        bg += createLabel(0, 1, "NoCmpr | Render: L | TA: C", Align.left, TextAlign.CENTRE, false)
        bg += createLabel(1, 1, "XCmpr | Render: L | TA: C", Align.left, TextAlign.CENTRE, true)
        bg += createLabel(2, 1, "Multi\nLine | Render: L | TA: C", Align.left, TextAlign.CENTRE, true)
        bg += createLabel(3, 1, "Do X Compression Enabled | Render: L | TA: C", Align.left, TextAlign.CENTRE, true)
        
        bg += createLabel(5, 1, "NoCmpr | Render: C | TA: C", Align.center, TextAlign.CENTRE, false)
        bg += createLabel(6, 1, "XCmpr | Render: C | TA: C", Align.center, TextAlign.CENTRE, true)
        bg += createLabel(7, 1, "Multi\nLine | Render: C | TA: C", Align.center, TextAlign.CENTRE, true)
        bg += createLabel(8, 1, "Do X Compression Enabled | Render: C | TA: C", Align.center, TextAlign.CENTRE, true)
        
        bg += createLabel(10, 1, "NoCmpr | Render: R | TA: C", Align.right, TextAlign.CENTRE, false)
        bg += createLabel(11, 1, "XCmpr | Render: R | TA: C", Align.right, TextAlign.CENTRE, true)
        bg += createLabel(12, 1, "Multi\nLine | Render: R | TA: C", Align.right, TextAlign.CENTRE, true)
        bg += createLabel(13, 1, "Do X Compression Enabled | Render: R | TA: C", Align.right, TextAlign.CENTRE, true)
        
        // Text Align: Right
        bg += createLabel(0, 2, "NoCmpr | Render: L | TA: R", Align.left, TextAlign.RIGHT, false)
        bg += createLabel(1, 2, "XCmpr | Render: L | TA: R", Align.left, TextAlign.RIGHT, true)
        bg += createLabel(2, 2, "Multi\nLine | Render: L | TA: R", Align.left, TextAlign.RIGHT, true)
        bg += createLabel(3, 2, "Do X Compression Enabled | Render: L | TA: R", Align.left, TextAlign.RIGHT, true)
        
        bg += createLabel(5, 2, "NoCmpr | Render: C | TA: R", Align.center, TextAlign.RIGHT, false)
        bg += createLabel(6, 2, "XCmpr | Render: C | TA: R", Align.center, TextAlign.RIGHT, true)
        bg += createLabel(7, 2, "Multi\nLine | Render: C | TA: R", Align.center, TextAlign.RIGHT, true)
        bg += createLabel(8, 2, "Do X Compression Enabled | Render: C | TA: R", Align.center, TextAlign.RIGHT, true)
        
        bg += createLabel(10, 2, "NoCmpr | Render: R | TA: R", Align.right, TextAlign.RIGHT, false)
        bg += createLabel(11, 2, "XCmpr | Render: R | TA: R", Align.right, TextAlign.RIGHT, true)
        bg += createLabel(12, 2, "Multi\nLine | Render: R | TA: R", Align.right, TextAlign.RIGHT, true)
        bg += createLabel(13, 2, "Do X Compression Enabled | Render: R | TA: R", Align.right, TextAlign.RIGHT, true)
    }

    override fun render(delta: Float) {
        super.render(delta)

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            populate()
        }

        val batch = main.batch
        batch.projectionMatrix = camera.combined
        batch.begin()

        root.renderAsRoot(batch)

        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.update()
        root.resize()
    }

    override fun show() {
        super.show()
        main.inputMultiplexer.removeProcessor(root.inputSystem)
        main.inputMultiplexer.addProcessor(root.inputSystem)
    }

    override fun hide() {
        super.hide()
        main.inputMultiplexer.removeProcessor(root.inputSystem)
    }

    override fun getDebugString(): String {
        val inp = root.inputSystem
        val vector = inp.mouseVector
        val newPath = root.contextMenuLayer.lastHoveredElementPath
//        val newPath = root.pathTo(vector.x, vector.y)
        return """InputSystem:
  vec: $vector
Input:
  x: ${Gdx.input.x}
  y: ${Gdx.input.y}
PathTest:
  pathSize: ${newPath.size}
  path: ${newPath.map { /*it.bounds*/ it.javaClass.simpleName }}
"""
    }

    override fun dispose() {
    }
}