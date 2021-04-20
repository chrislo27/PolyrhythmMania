package io.github.chrislo27.paintbox.tests.newui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.PaintboxScreen
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.font.TextBlock
import io.github.chrislo27.paintbox.font.TextRun
import io.github.chrislo27.paintbox.registry.AssetRegistry
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.border.SolidBorder
import io.github.chrislo27.paintbox.ui.control.Button
import io.github.chrislo27.paintbox.ui.control.TextLabel
import io.github.chrislo27.paintbox.ui.element.RectElement
import io.github.chrislo27.paintbox.util.MathHelper
import polyrhythmmania.util.DecimalFormats

internal class UIAnchorTestEditorScreen(override val main: NewUITestGame) : PaintboxScreen() {

    private val camera: OrthographicCamera = OrthographicCamera()
    private var root: SceneRoot = SceneRoot(Gdx.graphics.width, Gdx.graphics.height)
        private set(value) {
            main.inputMultiplexer.removeProcessor(field.inputSystem)
            field = value
            main.inputMultiplexer.addProcessor(value.inputSystem)
        }

    init {
        populate()
    }

    private fun populate() {
        root = SceneRoot(Gdx.graphics.width, Gdx.graphics.height)
        val bg = TestColorElement(Color(1f, 165f / 255f, 0.5f, 1f))
        root += bg

//        root.contextMenuLayer.root += TextLabel("Test context menu layer\n\n\n\n", font = main.debugFontBoldItalic).apply {
//            this.renderAlign.set(Align.center)
//            this.textAlign.set(TextAlign.LEFT)
//            this.backgroundColor.set(Color(1f, 1f, 1f, 0.75f))
//            this.renderBackground.set(true)
//            this.bgPadding.set(10f)
//            this.textColor.set(Color.BLACK)
//            
//            this.bounds.width.set(350f)
//            this.bounds.height.set(350f)
//            Anchor.Centre.configure(this)
//        }
//        root.tooltipLayer.root += TextLabel("Test tooltip layer", font = main.debugFontBold).apply {
//            this.renderAlign.set(Align.center)
//            this.textAlign.set(TextAlign.LEFT)
//            this.backgroundColor.set(Color(0f, 0f, 0f, 0.75f))
//            this.renderBackground.set(true)
//            this.bgPadding.set(10f)
//            this.textColor.set(Color.WHITE)
//            
//            this.bounds.width.set(250f)
//            this.bounds.height.set(200f)
//            Anchor.Centre.configure(this)
//        }

        // Toolbar
        bg += TestColorElement(Color(0f, 0f, 0f, 0.5f)).also { toolbar ->
            toolbar.bounds.height.set(40f)

            // Left toolbar
            toolbar += UIElement().also { lt ->
                val num = 6
                val buttonWidth = 32f
                val buttonSpacing = 4f
//                lt.bounds.x.set(4f)
//                lt.bounds.y.set(4f)
//                lt.bounds.height.set(32f)
                lt.bounds.width.set((buttonWidth + buttonSpacing) * (num + 2) - buttonSpacing)

                (0 until num).forEach { i ->
                    lt += TestColorElement(Color(1f, 1f, 1f, 1f).fromHsv(360f * i / num, 0.9f, 0.8f)).apply {
                        this.bounds.x.set((buttonWidth + buttonSpacing * 2) * i)
                        this.bounds.y.set(0f)
                        this.bounds.width.set(buttonWidth + buttonSpacing * 2)
                        this.bounds.height.set(buttonWidth + buttonSpacing * 2)
                        this.margin.set(Insets(buttonSpacing))
                        this.doClickFlash = true
                    }
                }
            }

            // Centre toolbar
            toolbar += UIElement().also { ct ->
                val num = 3
                val buttonWidth = 32f
                val buttonSpacing = 4f

                ct.bounds.width.set((buttonWidth + buttonSpacing) * num - buttonSpacing)
                Anchor.TopCentre.configure(ct)
                ct.bounds.y.set(4f)
                ct.bounds.height.set(32f)

                (0 until num).forEach { i ->
                    ct += TestColorElement(if (i == 0) Color.YELLOW else if (i == 1) Color.GREEN else Color.RED).apply {
                        this.bounds.x.set(36f * i)
                        this.bounds.y.set(0f)
                        this.bounds.width.set(32f)
                        this.bounds.height.set(32f)
                        this.doClickFlash = true
                        if (i == num / 2) {
                            addChild(ImageNode(PaintboxGame.paintboxSpritesheet.logo128, ImageRenderingMode.FULL).apply {

                            })
                        }
                    }
                }
            }

            // Right toolbar
            toolbar += UIElement().also { rt ->
                val num = 6
                val buttonWidth = 32f
                val buttonSpacing = 4f
                Anchor.TopRight.configure(rt, -4f, 4f)
                rt.bounds.height.set(32f)
                rt.bounds.width.set((buttonWidth + buttonSpacing) * num - buttonSpacing)

                (0 until num).forEach { i ->
                    rt += TestColorElement(Color(1f, 1f, 1f, 1f).fromHsv(360f * i / num, 0.9f, 0.8f)).apply {
                        Anchor.TopRight.configure(this, offsetX = -36f * i)
                        this.bounds.width.set(32f)
                        this.bounds.height.set(32f)
                        this.doClickFlash = true
                    }
                }
            }
        }

        bg += RectElement(Color(0f, 0f, 0f, 0.5f)).also { rect ->
            Anchor.BottomCentre.configure(rect, offsetY = -75f)
            rect.bounds.width.set(500f)
            rect.bounds.height.set(500f)
            
            rect.padding.set(Insets(10f))
            rect.border.set(Insets(4f))
            rect.borderStyle.set(SolidBorder(Color.CYAN))
            rect.margin.set(Insets(5f))

            rect += TextNode(TextBlock(listOf(
                    TextRun(main.debugFontBoldBordered, "Hello\nline2\nline3g adwadwadwadaddadwd"),
                    TextRun(main.debugFontBoldBordered, "a separate TextRun", color = Color.RED),
                    TextRun(main.debugFontBoldBordered, "\n\n\nnananananananananananananananananananananananannananananana"),
            ))).apply {
                renderAlign.set(Align.center)
                doXCompression.set(true)
                doClipping.set(true)

//                this += RectElement(Color(0f, 0f, 0f, 0.5f)).also { r2 ->
//                    Anchor.Centre.configure(r2)
//                    r2.bounds.width.set(128f)
//                    r2.bounds.height.set(128f)
//                    r2 += TextNode(TextBlock(listOf(
//                            TextRun(main.debugFontBoldItalic, "This is an internal\ntext node"),
//                                                   ))).apply {
//                        renderAlign.set(Align.center)
//                        doXCompression.set(false)
//                        doClipping.set(true)
//                    }
//                }
            }
        }

        bg += TextLabel("Test text label.\nNewline.").also { label ->
            Anchor.TopLeft.configure(label, offsetX = 32f, offsetY = 48f)
            label.bounds.width.set(325f)
            label.bounds.height.set(100f)
            label.renderBackground.set(true)
//            label.textAlign.set(TextAlign.RIGHT)
            label.bgPadding.set(10f)
            label.doXCompression.set(true)
            label.renderAlign.set(Align.left)
            label.doClipping.set(true)
//            label.opacity.set(0.5f)
//            label.textColor.set(Color(0f, 1f, 0f, 1f))

            label.setOnAction {
                println("hi top label")
            }
        }

        bg += TextLabel("").also { label ->
            label.internalTextBlock.set(TextBlock(listOf(
                    TextRun(main.debugFont, "Regular font.\n"),
                    TextRun(main.debugFontItalic, "Italic font.\n"),
                    TextRun(main.debugFontBold, "Bold font.\n"),
                    TextRun(main.debugFontBoldItalic, "Bold-italic font."),
            )))
            Anchor.BottomLeft.configure(label, offsetX = 32f, offsetY = -32f)
            label.bounds.width.set(325f)
            label.bounds.height.set(110f)
            label.renderBackground.set(true)
//            label.textAlign.set(TextAlign.RIGHT)
            label.bgPadding.set(10f)
            label.doXCompression.set(true)
            label.renderAlign.set(Align.left)
            label.doClipping.set(true)
//            label.opacity.set(0.5f)
//            label.textColor.set(Color(0f, 1f, 0f, 1f))

            label.setOnAction {
                println("hi bottom label")
            }
        }

        bg += Button("Test button", font = main.debugFont).also { button ->
            Anchor.CentreLeft.configure(button, offsetX = 32f)
            button.bounds.width.set(150f)
            button.bounds.height.set(50f)
            
            button.tooltipElement.set(Tooltip("Test tooltip text.").apply {
                this.text.bind { 
                    this@UIAnchorTestEditorScreen.root.frameUpdateTrigger.use()
                    "Test: ${DecimalFormats.format("0.000", MathHelper.getTriangleWave(2f))}"
                }
            })

            button.setOnAction {
                println("hi button")
            }
        }
        bg += Button("Disabled button", font = main.debugFont).also { button ->
            Anchor.CentreLeft.configure(button, offsetX = 32f + 175f)
            button.bounds.width.set(150f)
            button.bounds.height.set(50f)
            button.disabled.set(true)

            button.setOnAction {
                println("hi button disabled")
            }
        }
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
        root.resize(camera)
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
        val newPath = root.pathTo(vector.x, vector.y)
        return """InputSystem:
  vec: $vector
Input:
  x: ${Gdx.input.x}
  y: ${Gdx.input.y}
PathTest:
  pathSize: ${newPath.size}
  path: ${newPath.map { it.bounds }}
"""
    }

    override fun dispose() {
    }
}