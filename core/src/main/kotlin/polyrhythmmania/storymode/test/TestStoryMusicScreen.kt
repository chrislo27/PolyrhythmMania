package polyrhythmmania.storymode.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.binding.Var
import paintbox.ui.Pane
import paintbox.ui.SceneRoot
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.CheckBox
import paintbox.ui.control.Slider
import paintbox.ui.element.RectElement
import paintbox.ui.layout.ColumnarPane
import paintbox.ui.layout.ColumnarVBox
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.gdxutils.grey
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.storymode.StorySession
import polyrhythmmania.storymode.music.StemMix
import polyrhythmmania.storymode.music.StemMixes
import polyrhythmmania.storymode.music.StoryMusicAssets.STEM_ID_DESKTOP_HARM
import polyrhythmmania.storymode.music.StoryMusicAssets.STEM_ID_DESKTOP_MAIN
import polyrhythmmania.storymode.music.StoryMusicAssets.STEM_ID_DESKTOP_PERC
import polyrhythmmania.storymode.music.StoryMusicAssets.STEM_ID_TITLE_FULL1
import polyrhythmmania.storymode.music.StoryMusicAssets.STEM_ID_TITLE_PERC1
import polyrhythmmania.storymode.music.StoryMusicHandler
import polyrhythmmania.storymode.screen.EarlyAccessMsgOnBottom


class TestStoryMusicScreen(
        main: PRManiaGame, val prevScreen: Screen,
) : PRManiaScreen(main), EarlyAccessMsgOnBottom {

    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val processor: InputProcessor = sceneRoot.inputSystem

    val storySession: StorySession = StorySession()
    val musicHandler: StoryMusicHandler = storySession.musicHandler
    
    val currentStemMix: Var<StemMix> = Var(musicHandler.getCurrentlyActiveStemMix())

    init {
        fun separator(): UIElement {
            return RectElement(Color().grey(90f / 255f, 0.8f)).apply {
                this.bounds.height.set(6f)
                this.margin.set(Insets(2f, 2f, 0f, 0f))
            }
        }

        sceneRoot += RectElement(PRManiaColors.debugColor).apply {
            this.padding.set(Insets(20f))

            this += ColumnarPane(listOf(1, 9), true).apply {
                this.spacing.set(10f)

                this[0] += Pane().apply {
                    this += Slider().apply {
                        this.bindHeightToParent(multiplier = 0.65f)
                        this.disabled.set(true)
                        this.tickUnit.set(0f)
                        this.minimum.set(0f)
                        this.maximum.set(StoryMusicHandler.DURATION_BEATS)
                        musicHandler.currentBeat.addListener { b ->
                            this.setValue(b.getOrCompute())
                        }
                    }
                }
                this[1] += ColumnarVBox(2, false).apply {
                    this.spacing.set(60f)
                    
                    this[0].apply {
                        this.spacing.set(4f)
                        this.temporarilyDisableLayouts {
                            this += Button("Fade out to nothing").apply {
                                this.bounds.height.set(32f)
                                this.setOnAction {
                                    musicHandler.fadeOut(0.5f)
                                }
                            }
                            this += separator()
                            this += Button("Transition to \"title screen\" mix").apply {
                                this.bounds.height.set(32f)
                                this.setOnAction {
                                    musicHandler.transitionToTitleMix()
                                }
                            }
                            this += Button("Transition to \"post-results\" mix").apply {
                                this.bounds.height.set(32f)
                                this.setOnAction {
                                    musicHandler.transitionToStemMix(musicHandler.getPostResultsStemMix(), 1f) // Real function has a delay
                                }
                            }
                            this += separator()
                            this += Button("Transition to \"pre-TRAINING-101 phase\" mix").apply {
                                this.bounds.height.set(32f)
                                this.setOnAction {
                                    musicHandler.transitionToStemMix(StemMixes.desktopPreTraining101, 1f)
                                }
                            }
                            this += Button("Transition to \"internship phase\" mix").apply {
                                this.bounds.height.set(32f)
                                this.setOnAction {
                                    musicHandler.transitionToStemMix(StemMixes.desktopInternship, 1f)
                                }
                            }
                            this += Button("Transition to \"main-game phase\" mix").apply {
                                this.bounds.height.set(32f)
                                this.setOnAction {
                                    musicHandler.transitionToStemMix(StemMixes.desktopMain, 1f)
                                }
                            }
                            this += Button("Transition to \"boss quiet\" mix").apply {
                                this.bounds.height.set(32f)
                                this.setOnAction {
                                    musicHandler.transitionToStemMix(StemMixes.desktopPreBossQuiet, 1f)
                                }
                            }
                            this += Button("Transition to \"post-boss silent\" mix").apply {
                                this.bounds.height.set(32f)
                                this.setOnAction {
                                    musicHandler.transitionToStemMix(StemMixes.desktopPostBossSilent, 1f)
                                }
                            }
                            this += Button("Transition to \"post-boss main\" mix").apply {
                                this.bounds.height.set(32f)
                                this.setOnAction {
                                    musicHandler.transitionToStemMix(StemMixes.desktopPostBossMain, 1f)
                                }
                            }
                            this += Button("Transition to \"post-game phase\" mix").apply {
                                this.bounds.height.set(32f)
                                this.setOnAction {
                                    musicHandler.transitionToStemMix(StemMixes.desktopPostGame, 1f)
                                }
                            }
                        }
                    }
                    this[1].apply {
                        this.spacing.set(4f)
                        this.temporarilyDisableLayouts {
                            fun createStemCheckBox(stemID: String): CheckBox {
                                return CheckBox(stemID).apply {
                                    this.bounds.height.set(32f)
                                    this.setOnAction {
                                        checkedState.invert()

                                        val current = currentStemMix.getOrCompute()
                                        val newStemMix = current.copy(stemIDs = if (this.selectedState.get())
                                            (current.stemIDs + stemID)
                                        else (current.stemIDs - stemID))
                                        musicHandler.transitionToStemMix(newStemMix, 1f)
                                    }
                                }
                            }
                            listOf(
                                    STEM_ID_TITLE_FULL1,
                                    STEM_ID_TITLE_PERC1,
                                    STEM_ID_DESKTOP_HARM,
                                    STEM_ID_DESKTOP_MAIN,
                                    STEM_ID_DESKTOP_PERC,
                            ).forEach { stemID ->
                                this += createStemCheckBox(stemID).apply { 
                                    currentStemMix.addListener {
                                        val isIncluded = stemID in it.getOrCompute().stemIDs
                                        this.selectedState.set(isIncluded)
                                    }
                                }
                            }
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

        val batch = main.batch
        val camera = uiCamera
        batch.projectionMatrix = camera.combined
        batch.begin()
        uiViewport.apply()
        sceneRoot.renderAsRoot(batch)
        batch.end()
    }

    override fun shouldMakeTextTransparent(): Boolean {
        return true
    }

    override fun renderUpdate() {
        super.renderUpdate()

        musicHandler.frameUpdate()
        this.currentStemMix.set(musicHandler.getCurrentlyActiveStemMix())

        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            main.screen = prevScreen
            this.disposeQuietly()
        }
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
        musicHandler.fadeOutAndDispose(0f)
    }
}
