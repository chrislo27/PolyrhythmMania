package paintbox.tests.newui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.Align
import paintbox.PaintboxGame
import paintbox.PaintboxScreen
import paintbox.font.TextAlign
import paintbox.font.TextBlock
import paintbox.font.TextRun
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.SeparatorMenuItem
import paintbox.ui.contextmenu.SimpleMenuItem
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.util.MathHelper
import paintbox.util.gdxutils.isControlDown
import paintbox.util.DecimalFormats

internal class UIAnchorTestEditorScreen(override val main: NewUITestGame) : PaintboxScreen() {

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
                        this.padding.set(Insets(2f))
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
            Anchor.BottomCentre.configure(rect, offsetX = 0f, offsetY = -75f)
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
                doXCompression.set(false)
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
            label.bounds.height.set(75f)
            label.renderBackground.set(true)
//            label.textAlign.set(TextAlign.RIGHT)
            label.bgPadding.set(Insets(10f))
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
//            label.setScaleXY(0.5f)
            Anchor.BottomLeft.configure(label, offsetX = 32f, offsetY = -32f)
            label.bounds.width.set(325f)
            label.bounds.height.set(110f)
            label.renderBackground.set(true)
            label.renderAlign.set(Align.topLeft)
            label.textAlign.set(TextAlign.LEFT)
            label.bgPadding.set(Insets(10f))
//            label.bgPadding.set(0f)
            label.doXCompression.set(true)
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

            (button.skin.getOrCompute() as ButtonSkin).roundedRadius.set(10)
            button.tooltipElement.set(Tooltip("Test tooltip text.").apply {
                this.text.bind {
//                    this@UIAnchorTestEditorScreen.root.frameUpdateTrigger.use()
                    "Test: ${DecimalFormats.format("0.000", MathHelper.getTriangleWave(2f))}"
                }
            })

            button.setOnAction {
                println("hi button")
            }
        }

        bg += Pane().also { pane ->
            Anchor.CentreLeft.configure(pane, offsetX = 32f, offsetY = 75f)
            pane.bounds.width.set(150f)
            pane.bounds.height.set(50f)
            pane.padding.set(Insets(5f))
            pane.margin.set(Insets(2f))
            pane.border.set(Insets(4f))
            pane.borderStyle.set(SolidBorder().apply { this.color.set(Color.MAGENTA) })
            pane.addChild(Button("Test button bounds", font = main.debugFont).also { button ->
                (button.skin.getOrCompute() as ButtonSkin).roundedRadius.set(0)
            })
        }
        bg += Button("Context menu button", font = main.debugFont).also { button ->
            Anchor.CentreLeft.configure(button, offsetX = 32f + 175f, offsetY = 75f)
            button.bounds.width.set(150f)
            button.bounds.height.set(50f)

            button.setOnRightClick { event ->
                val root = button.sceneRoot.getOrCompute()
                if (root != null) {
                    root.showRootContextMenu(ContextMenu().apply {
                        this.addMenuItem(SimpleMenuItem.create("First SimpleMenuItem", main.debugFont))
                        this.addMenuItem(SimpleMenuItem.create("Second SimpleMenuItem", main.debugFont))
                        this.addMenuItem(SeparatorMenuItem())
                        this.addMenuItem(SimpleMenuItem.create("Third SimpleMenuItem", main.debugFont))
                    })
                }
            }
        }
        bg += CheckBox("Check box test", font = main.debugFont).also { button ->
            Anchor.CentreLeft.configure(button, offsetX = 32f, offsetY = 75f * 2)
            button.bounds.width.set(150f)
            button.bounds.height.set(50f)
        }
        val toggleGroup = ToggleGroup()
        bg += RadioButton("Radio button 1", font = main.debugFont).also { button ->
            Anchor.CentreLeft.configure(button, offsetX = 32f + 175f, offsetY = 75f * 2 - 12.5f)
            button.bounds.width.set(150f)
            button.bounds.height.set(25f)
            toggleGroup.addToggle(button)
        }
        bg += RadioButton("Radio button 2", font = main.debugFont).also { button ->
            Anchor.CentreLeft.configure(button, offsetX = 32f + 175f, offsetY = 75f * 2 - 12.5f + 25f)
            button.bounds.width.set(150f)
            button.bounds.height.set(25f)
            toggleGroup.addToggle(button)
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
        bg += Button("Test button right", font = main.debugFont).also { button ->
            Anchor.CentreRight.configure(button, offsetX = -16f)
            button.bounds.width.set(150f)
            button.bounds.height.set(50f)

            button.tooltipElement.set(Tooltip("Test very long extremely long tooltip text."))

            button.setOnAction {
                println("hi button right")
            }
        }
        bg += Button("Test button top", font = main.debugFont).also { button ->
            Anchor.TopCentre.configure(button, offsetX = 200f)
            button.bounds.width.set(150f)
            button.bounds.height.set(50f)

            button.tooltipElement.set(Tooltip("Test very long extremely long tooltip text."))

            button.setOnAction {
                println("hi button top")
            }
        }
//        bg += Pane().also { outerPane ->
//            Anchor.CentreRight.configure(outerPane, offsetX = -200f)
//            outerPane.bounds.width.set(150f)
//            outerPane.bounds.height.set(150f)
//
//            outerPane.border.set(Insets(2f))
//            outerPane.borderStyle.set(SolidBorder(Color(1f, 1f, 1f, 1f)))
//
//            val r = RectElement(Color(0f, 0f, 0f, 0.75f))
//            outerPane.addChild(r)
//
//            val scrollPane = Pane().also { scrollPane ->
//                scrollPane.doClipping.set(true)
//                
////                scrollPane.addChild(RectElement(Color(0f, 1f, 0f, 1f)).apply { 
////                    this.bounds.width.set(25f)
////                    this.bounds.height.set(25f)
////                    this.bounds.x.set(25f)
////                    this.bounds.y.set(25f)
////                })
//                scrollPane.addChild(Button("Button").apply { 
//                    this.bounds.width.set(50f)
//                    this.bounds.height.set(25f)
//                    this.bounds.x.set(150f)
//                    this.bounds.y.set(125f)
//                    this.setOnAction { 
//                        println("Button triggered inside pseudo-scroll pane")
//                    }
//                })
//                scrollPane.addChild(TextLabel("Here's some tall text that was automatically wrapped. This could be something like in a thin newspaper column.").apply {
//                    this.bounds.width.set(130f)
//                    this.bounds.height.set(300f)
//                    this.bounds.x.set(10f)
//                    this.bounds.y.set(60f)
//                    this.textColor.set(Color.WHITE)
//                    this.doLineWrapping.set(true)
//                    this.renderAlign.set(Align.topLeft)
//                    this.textAlign.set(TextAlign.LEFT)
//                })
////                scrollPane.addChild(TextLabel("Here's some long text that was also automatically wrapped. This could be something like in a big paragraph of text.").apply {
////                    this.bounds.width.set(300f)
////                    this.bounds.height.set(300f)
////                    this.bounds.x.set(10f)
////                    this.bounds.y.set(60f)
////                    this.textColor.set(Color.WHITE)
////                    this.doLineWrapping.set(true)
////                    this.renderAlign.set(Align.topLeft)
////                    this.textAlign.set(TextAlign.LEFT)
////                })
//            }
//            r.addChild(scrollPane)
//
//            r.addInputEventListener { event ->
//                when (event) {
//                    is MouseMoved -> {
//                        val lastMouseRelative = Vector2(0f, 0f)
//                        val thisPos = r.getPosRelativeToRoot(lastMouseRelative)
//                        lastMouseRelative.x = event.x - thisPos.x
//                        lastMouseRelative.y = event.y - thisPos.y
//
//                        scrollPane.contentOffsetX.set(-lastMouseRelative.x)
//                        scrollPane.contentOffsetY.set(-lastMouseRelative.y)
//
//                        true
//                    }
//                    else -> false
//                }
//            }
//        }
        
//        bg += ScrollBar(ScrollBar.Orientation.VERTICAL).apply {
//            Anchor.CentreRight.configure(this, offsetX = -100f, offsetY = -200f)
//            this.bounds.width.set(20f)
//            this.bounds.height.set(200f)
//        }
//        bg += ScrollBar(ScrollBar.Orientation.HORIZONTAL).apply {
//            Anchor.CentreRight.configure(this, offsetX = -100f - 50f, offsetY = -100f)
//            this.bounds.width.set(200f)
//            this.bounds.height.set(20f)
//        }
        bg += ScrollPane().apply {
            Anchor.CentreRight.configure(this, offsetX = -100f - 50f, offsetY = -200f)
            this.bounds.width.set(200f)
            this.bounds.height.set(200f)

            setContent(Pane().also { outerPane ->
                outerPane.bounds.width.set(300f)
                outerPane.bounds.height.set(500f)
                outerPane.border.set(Insets(2f))
                outerPane.borderStyle.set(SolidBorder(Color(1f, 1f, 1f, 1f)))

                val r = RectElement(Color(0f, 0f, 0f, 0.75f))
                outerPane.addChild(r)

                val scrollPane = Pane().also { scrollPane ->
                    scrollPane.addChild(Button("Btn").apply {
                        this.bounds.width.set(50f)
                        this.bounds.height.set(25f)
                        this.bounds.x.set(150f)
                        this.bounds.y.set(125f)
                        this.setOnAction {
                            println("Button triggered inside pseudo-scroll pane")
                        }
                    })
                    scrollPane.addChild(TextLabel("Here's some tall text that was automatically wrapped. This could be something like in a thin newspaper column.").apply {
                        this.bounds.width.set(130f)
                        this.bounds.height.set(300f)
                        this.bounds.x.set(10f)
                        this.bounds.y.set(60f)
                        this.textColor.set(Color.WHITE)
                        this.doLineWrapping.set(true)
                        this.renderAlign.set(Align.topLeft)
                        this.textAlign.set(TextAlign.LEFT)
                    })
//                scrollPane.addChild(TextLabel("Here's some long text that was also automatically wrapped. This could be something like in a big paragraph of text.").apply {
//                    this.bounds.width.set(300f)
//                    this.bounds.height.set(300f)
//                    this.bounds.x.set(10f)
//                    this.bounds.y.set(60f)
//                    this.textColor.set(Color.WHITE)
//                    this.doLineWrapping.set(true)
//                    this.renderAlign.set(Align.topLeft)
//                    this.textAlign.set(TextAlign.LEFT)
//                })
                }
                r.addChild(scrollPane)
            })
        }
        
        bg += Slider().apply {
            Anchor.CentreRight.configure(this, offsetX = -100f - 50f, offsetY = 100f)
            this.bounds.width.set(200f)
            this.bounds.height.set(30f)
        }
        
        bg += RectElement(Color(0f, 0f, 0f, 0.75f)).apply {
            Anchor.TopLeft.configure(this, offsetX = 50f, offsetY = 150f)
            this.bounds.width.set(200f)
            this.bounds.height.set(30f)
            this += TextField().apply { 
                this.textColor.set(Color(1f, 1f, 1f, 1f))
                this.padding.set(Insets(2f))
                this.text.set("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
            }
        }
        
        bg += RectElement(Color(0f, 0f, 0f, 0.75f)).apply {
            Anchor.TopLeft.configure(this, offsetX = 50f, offsetY = 190f)
            this.bounds.width.set(200f)
            this.bounds.height.set(30f)
            this += TextField().apply { 
                this.textColor.set(Color(1f, 1f, 1f, 1f))
                this.padding.set(Insets(2f))
                this.text.set("Second text field Second text field Second text field")
            }
        }
        
    }

    override fun render(delta: Float) {
        super.render(delta)

        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && Gdx.input.isControlDown()) {
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