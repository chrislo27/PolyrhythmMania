package polyrhythmmania.storymode.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.ui.Anchor
import paintbox.ui.ImageIcon
import paintbox.ui.SceneRoot
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.element.RectElement
import paintbox.ui.layout.ColumnarPane
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.StorySavefile


class StoryTitleScreen(main: PRManiaGame) : PRManiaScreen(main) {
    
    
    val batch: SpriteBatch = main.batch
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val processor: InputProcessor = sceneRoot.inputSystem

    val savefiles: List<StorySavefile.SavefileLoaded> = (1..3).map(StorySavefile.Companion::attemptLoad)
    
    init {
        // FIXME
        sceneRoot += RectElement(Color(1f, 165f / 255f, 0.5f, 1f)).apply { 
            this += ColumnarPane(listOf(5, 5), true).apply {
                this.spacing.set(48f)
                this.columnBoxes.forEach { pane ->
                    pane.margin.set(Insets(4f))
                }
                this[0] += ImageIcon(TextureRegion(StoryAssets.get<Texture>("logo"))).apply {
                    this.margin.set(Insets(4f))
                }
                this[1] += ColumnarPane(listOf(6, 4), true).apply { 
                    this[0] += ColumnarPane(savefiles.size, false).apply { 
                        this.bindWidthToParent(multiplier = 0.5f)
                        this.bindHeightToParent(multiplier = 0.75f)
                        Anchor.Centre.configure(this)
                        this.spacing.set(10f)

                        this.columnBoxes.zip(savefiles).forEachIndexed { index, (pane, savefile) ->
                            pane += Button("File ${savefile.number}\n\n${savefile.javaClass.simpleName}").apply {
                                Anchor.Centre.configure(this)
                                this.bindWidthToSelfHeight()
                            }
                        }
                    }
                    this[1] += Button(StoryL10N.getVar("titleScreen.quitToMainMenu")).apply { 
                        Anchor.Centre.configure(this)
                        this.bounds.width.set(300f)
                        this.bounds.height.set(64f)
                        this.setOnAction {
                            main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_deselect"))
                            
                            val doAfterUnload: () -> Unit = {
                                val mainMenu = main.mainMenuScreen.prepareShow(doFlipAnimation = true)

                                main.screen = TransitionScreen(main, main.screen, mainMenu, FadeToOpaque(0.125f, Color.BLACK), null)
                            }
                            main.screen = TransitionScreen(main, main.screen, StoryLoadingScreen(main, true, doAfterUnload),
                                    FadeToOpaque(0.25f, Color.BLACK), FadeToTransparent(0.125f, Color.BLACK))
                        }
                    }
                }
            }
        }
    }
    
    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        super.render(delta)

        val camera = uiCamera
        batch.projectionMatrix = camera.combined
        batch.begin()
        
        sceneRoot.renderAsRoot(batch)
        
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        uiViewport.update(width, height)
    }

    override fun show() {
        super.show()
        main.inputMultiplexer.removeProcessor(processor)
        main.inputMultiplexer.addProcessor(processor)
    }

    override fun hide() {
        super.hide()
        main.inputMultiplexer.removeProcessor(processor)
    }

    override fun dispose() {
    }
}