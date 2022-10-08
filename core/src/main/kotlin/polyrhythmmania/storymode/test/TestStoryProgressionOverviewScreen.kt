package polyrhythmmania.storymode.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.Paintbox
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.Button
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.storymode.inbox.InboxDB
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.state.InboxState
import polyrhythmmania.storymode.inbox.unlock.Progression
import polyrhythmmania.storymode.inbox.unlock.StoryModeProgression
import polyrhythmmania.storymode.inbox.unlock.UnlockStage
import polyrhythmmania.storymode.screen.StoryPlayScreen


/**
 * Progression overview debug screen
 */
class TestStoryProgressionOverviewScreen(main: PRManiaGame, val prevScreen: Screen)
    : PRManiaScreen(main) {
    
    val batch: SpriteBatch = main.batch
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val processor: InputProcessor = sceneRoot.inputSystem

    val progression: Progression = StoryModeProgression(InboxState())
    
    init {
        val bg = RectElement(PRManiaColors.debugColor).apply {
            this.padding.set(Insets(8f))    
            this += Button("Back").apply {
                this.bounds.width.set(100f)
                this.bounds.height.set(32f)    
                this.setOnAction { 
                    main.screen = prevScreen
                }
            }
        }
        sceneRoot += bg
        
        bg += TextLabel("Progression Overview (debug)", font = main.fontMainMenuHeading).apply { 
            this.bounds.width.set(500f)
            this.bounds.height.set(60f)
            Anchor.TopCentre.configure(this)
            this.renderAlign.set(RenderAlign.center)
        }

        val pane = Pane().apply {
            Anchor.BottomCentre.configure(this)
            this.bounds.width.set(1100f)
            this.bounds.height.set(640f)
        }
        bg += pane
        val scrollPane = ScrollPane().apply {
            this.vBar.blockIncrement.set(100f)
            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(Color.CYAN).lerp(Color.WHITE, 0.9f))
        }
        val vbox = VBox().apply {
            this.temporarilyDisableLayouts { 
                fun createHBox(vararg children: UIElement): HBox {
                    return HBox().apply { 
                        this.align.set(HBox.Align.CENTRE)
                        this.spacing.set(20f)
                        
                        this.temporarilyDisableLayouts { 
                            children.forEach { c -> this += c }
                        }
                        this.sizeHeightToChildren()
                    }
                }
                fun createStageBox(unlockStage: UnlockStage): UIElement {
                    return VBox().apply {
                        this.border.set(Insets(8f))
                        this.borderStyle.set(SolidBorder(Color.BLACK))
                        this.padding.set(Insets(10f))
                        
                        this.spacing.set(5f)

                        this += TextLabel("Stage ID: ${unlockStage.id}").apply {
                            this.bounds.width.set(200f)
                            this.bounds.height.set(32f)
                        }
                        this += HBox().apply {
                            this.spacing.set(32f)
                            this.bounds.height.set(100f)

                            fun createReqOptBox(color: Color, items: List<String>) {
                                this += VBox().apply {
                                    this.border.set(Insets(4f))
                                    this.borderStyle.set(SolidBorder(color))
                                    this.padding.set(Insets(2f, 4f))

                                    this.bounds.width.set(200f)
                                    this.spacing.set(4f)

                                    items.forEach { id ->
                                        this += Button(id).apply {
                                            this.border.set(Insets(1f))
                                            this.borderStyle.set(SolidBorder(Color.BLACK))
                                            this.bounds.height.set(32f)
                                            this.setOnAction {
                                                val item = InboxDB.allItems.getValue(id) as InboxItem.ContractDoc
                                                main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_enter_game"))
                                                val gameMode = item.contract.gamemodeFactory(main)
                                                val playScreen = StoryPlayScreen(main, gameMode.container, Challenges.NO_CHANGES,
                                                        main.settings.inputCalibration.getOrCompute(), gameMode, item.contract, this@TestStoryProgressionOverviewScreen) {
                                                    Paintbox.LOGGER.debug("ExitReason: $it")
                                                }
                                                if (Gdx.input.isShiftDown()) {
                                                    playScreen.container.engine.autoInputs = true
                                                }
                                                main.screen = TransitionScreen(main, main.screen, playScreen,
                                                        FadeToOpaque(0.25f, Color.BLACK), FadeToTransparent(0.25f, Color.BLACK)).apply {
                                                    this.onEntryEnd = {
                                                        gameMode.prepareFirstTime()
                                                        playScreen.resetAndUnpause()
                                                        playScreen.initializeIntroCard()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            createReqOptBox(Color.RED, unlockStage.requiredInboxItems)
                            if (unlockStage.optionalInboxItems.isNotEmpty()) {
                                createReqOptBox(Color.BLUE, unlockStage.optionalInboxItems)
                            }

                            this.sizeWidthToChildren(100f)
                        }
                        this += TextLabel("# required to complete: ${if (unlockStage.minRequiredToComplete == unlockStage.requiredInboxItems.size) "all" else unlockStage.minRequiredToComplete}").apply {
                            this.bounds.width.set(200f)
                            this.bounds.height.set(32f)
                        }
                        
                        this.sizeWidthToChildren(100f)
                        this.sizeHeightToChildren(100f)
                    }
                }
                fun createLine(): UIElement {
                    return RectElement(Color.RED).apply { 
                        this.bounds.width.set(10f)
                        this.bounds.height.set(50f)
                    }
                }
                
                this.temporarilyDisableLayouts {
                    progression.stages.flatMapIndexed { index, unlockStage ->
                        listOf(
                                createHBox(createStageBox(unlockStage)),
                                createHBox(createLine())
                        )
                    }.dropLast(1).forEach { this += it }
                }
            }
            this.sizeHeightToChildren()
        }
        scrollPane.setContent(vbox)
        pane += scrollPane
        
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