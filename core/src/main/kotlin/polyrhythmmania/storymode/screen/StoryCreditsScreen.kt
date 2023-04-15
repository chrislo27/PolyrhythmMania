package polyrhythmmania.storymode.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.font.Markup
import paintbox.font.TextAlign
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.RenderAlign
import paintbox.ui.SceneRoot
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.ColumnarPane
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.storymode.StoryCredits
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.StorySession
import polyrhythmmania.storymode.screen.desktop.DesktopStyledPane
import polyrhythmmania.storymode.screen.desktop.DesktopUI.Companion.UI_SCALE


class StoryCreditsScreen(main: PRManiaGame, val storySession: StorySession, val onExit: () -> Unit) :
    PRManiaScreen(main) {

    companion object {

        private val HEADING_TEXT_COLOR: Color = Color.valueOf("FFE97F")
        private val NAME_TEXT_COLOR: Color = Color.valueOf("D8D8D8")
    }

    val batch: SpriteBatch = main.batch
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val processor: InputProcessor = sceneRoot.inputSystem

    init {
        val parent = RectElement(Color.BLACK).apply {
            this.margin.set(Insets(10f * UI_SCALE))
        }
        sceneRoot += parent

        parent += DesktopStyledPane().apply {
            Anchor.BottomCentre.configure(this)
            this.bounds.width.set(54f * UI_SCALE)
            this.bounds.height.set(22f * UI_SCALE)
            this.padding.set(Insets(7f * UI_SCALE, 5f * UI_SCALE, 10f * UI_SCALE, 10f * UI_SCALE))

            this.textureToUse = DesktopStyledPane.DEFAULT_TEXTURE_TO_USE_DARK

            this += Button(StoryL10N.getVar("credits.advance"), font = main.fontMainMenuMain).apply {
                this.setOnAction {
                    main.playMenuSfx(AssetRegistry.get<Sound>("sfx_pause_exit"))
                    onExit()
                }
            }
        }

        val pane = Pane().apply {
            this.bindHeightToParent(adjust = -32f * UI_SCALE)

            this += TextLabel(StoryL10N.getVar("credits.heading"), font = main.fontMainMenuHeading).apply {
                this.bounds.height.set(12f * UI_SCALE)
                this.textColor.set(HEADING_TEXT_COLOR)
                this.renderAlign.set(RenderAlign.center)
            }
            this += ColumnarPane(4, false).apply {
                Anchor.BottomLeft.configure(this)
                this.margin.set(Insets(0f, 12f * UI_SCALE))
                this.bindHeightToParent(adjust = -24f * UI_SCALE) 
                
                val markup = Markup.createWithBoldItalic(main.fontMainMenuMain, null, main.fontMainMenuItalic, null)
                fun makeTextLabel(indices: IntRange): TextLabel {
                    return TextLabel("").apply {
                        this.renderAlign.set(RenderAlign.top)
                        this.textAlign.set(TextAlign.LEFT)
                        this.markup.set(markup)
                        this.textColor.set(NAME_TEXT_COLOR)

                        this.text.bind {
                            StoryCredits.credits.toList().slice(indices).joinToString(separator = "\n\n") { (heading, values) ->
                                "[color=#${HEADING_TEXT_COLOR}]${heading.use()}[]\n${values.joinToString(separator = "\n") { v -> v.use() }}"
                            }
                        }
                    }
                }
                this[0] += makeTextLabel(0..2)
                this[1] += makeTextLabel(3..3)
                this[2] += makeTextLabel(4..4)
                this[3] += makeTextLabel(5..6)
            }
        }
        parent += pane
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        super.render(delta)

        val camera = uiCamera
        val batch = this.batch
        batch.projectionMatrix = camera.combined
        batch.begin()

        uiViewport.apply()
        batch.setColor(1f, 1f, 1f, 1f)
        sceneRoot.renderAsRoot(batch)

        batch.end()
    }

    override fun renderUpdate() {
        super.renderUpdate()

        storySession.renderUpdate()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        uiViewport.update(width, height)
    }

    override fun show() {
        super.show()

        main.inputMultiplexer.removeProcessor(processor)
        main.inputMultiplexer.addProcessor(processor)

        Gdx.input.isCursorCatched = false
    }

    override fun hide() {
        super.hide()

        main.inputMultiplexer.removeProcessor(processor)
    }

    override fun dispose() {
    }
}