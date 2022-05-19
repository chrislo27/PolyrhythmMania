package polyrhythmmania.storymode.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.ui.SceneRoot
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.element.RectElement
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.editor.EditorScreen
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.storymode.screen.StoryLoadingScreen
import polyrhythmmania.storymode.screen.StoryTitleScreen
import polyrhythmmania.storymode.test.gamemode.TestStory8BallGameMode
import polyrhythmmania.storymode.test.gamemode.TestStoryAcesOnlyGameMode
import polyrhythmmania.storymode.test.gamemode.TestStoryGameMode
import polyrhythmmania.storymode.test.gamemode.TestStoryNoBarelyGameMode
import java.util.*


class TestStoryGimmickDebugScreen(main: PRManiaGame) : PRManiaScreen(main) {
    
    val batch: SpriteBatch = main.batch
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val processor: InputProcessor = sceneRoot.inputSystem

    
    init {
        sceneRoot += RectElement(PRManiaColors.debugColor).apply {
            this.padding.set(Insets(32f, 10f, 12f, 12f))    
            this += VBox().apply { 
                this.spacing.set(4f)
                this.bindWidthToParent(multiplier = 0.5f)
                this.temporarilyDisableLayouts {
                    fun separator(): UIElement {
                        return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                            this.bounds.height.set(10f)
                            this.margin.set(Insets(4f, 4f, 0f, 0f))
                        }
                    }
                    
                    this += Button("Back to Main Menu").apply { 
                        this.bounds.height.set(32f)
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
                    this += separator()
                    this += Button("Story Mode Editor").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            Gdx.app.postRunnable {
                                val editorScreen = EditorScreen(main, EnumSet.of(EditorSpecialFlags.STORY_MODE))
                                main.screen = TransitionScreen(main, main.screen, editorScreen,
                                    FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }
                    this += separator()
                    this += Button("Story Mode file select").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            Gdx.app.postRunnable {
                                val titleScreen = StoryTitleScreen(main)
                                main.screen = TransitionScreen(main, main.screen, titleScreen,
                                        FadeToOpaque(0.125f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK))
                            }
                        }
                    }
                    this += separator()
                    this += Button("Polyrhythm 2 no changes").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            enterGameMode(object : TestStoryGameMode(main) {
                            })
                        }
                    }
                    this += Button("Aces Only PR2").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            enterGameMode(TestStoryAcesOnlyGameMode(main))
                        }
                    }
                    this += Button("No Barelies PR2").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            enterGameMode(TestStoryNoBarelyGameMode(main))
                        }
                    }
                    this += Button("Continuous (8-ball) PR2").apply {
                        this.bounds.height.set(32f)
                        this.setOnAction {
                            enterGameMode(TestStory8BallGameMode(main))
                        }
                    }
                }
            }
        }
    }
    
    private fun enterGameMode(gameMode: TestStoryGameMode) {
        main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_enter_game"))
        val playScreen = TestStoryPlayScreen(main, Challenges.NO_CHANGES, main.settings.inputCalibration.getOrCompute(), gameMode)
        main.screen = TransitionScreen(main, main.screen, playScreen,
                FadeToOpaque(0.25f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK)).apply {
            this.onEntryEnd = {
                gameMode.prepareFirstTime()
                playScreen.resetAndUnpause()
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