package polyrhythmmania.storymode.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.Paintbox
import paintbox.binding.Var
import paintbox.font.Markup
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.ScrollPane
import paintbox.ui.control.ScrollPaneSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.ColumnarPane
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.storymode.StorySavefile
import polyrhythmmania.storymode.StorySession
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.progression.Progression
import polyrhythmmania.storymode.screen.StoryPlayScreen
import polyrhythmmania.storymode.screen.desktop.DesktopScenario
import polyrhythmmania.storymode.screen.desktop.InboxItemRenderer
import polyrhythmmania.ui.PRManiaSkins


/**
 * Original all inbox items screen. Do not modify, just keeping as a reference.
 * (Replaced with [TestStoryDesktopScreen])
 */
class TestStoryAllInboxItemsScreen(main: PRManiaGame, val storySession: StorySession, val prevScreen: Screen)
    : PRManiaScreen(main) {
    
    val batch: SpriteBatch = main.batch
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val processor: InputProcessor = sceneRoot.inputSystem

    private val desktopScenario: DesktopScenario = DesktopScenario(DebugAllInboxItemsDB, Progression(emptyList()),
            storySession.currentSavefile ?: StorySavefile.newDebugSaveFile())
    private val inboxItemRenderer: InboxItemRenderer = InboxItemRenderer(main, desktopScenario)
    private val monoMarkup: Markup get() = inboxItemRenderer.monoMarkup
    private val slabMarkup: Markup get() = inboxItemRenderer.slabMarkup
    private val robotoCondensedMarkup: Markup get() = inboxItemRenderer.robotoCondensedMarkup
    private val openSansMarkup: Markup get() = inboxItemRenderer.openSansMarkup
    
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

        val pane = Pane().apply {
            Anchor.Centre.configure(this)
            this.bounds.width.set(1100f)
            this.bounds.height.set(600f)
        }
        bg += pane
        val columns = ColumnarPane(listOf(1, 2), false).apply {
            this.spacing.set(16f)
        }
        pane += columns
        
        val currentInboxFolder: Var<InboxItem?> = Var(null)
        columns[0] += RectElement(Color(0f, 0f, 0f, 0.5f)).apply { 
            this += TextLabel("Inbox Tray", font = main.fontMainMenuHeading).apply {
                this.bounds.height.set(70f)
                this.textColor.set(Color.WHITE)
                this.renderAlign.set(Align.left)
                this.padding.set(Insets(8f, 8f, 16f, 8f))
            }
            this += Pane().apply { 
                this.bounds.y.set(70f)
                this.bindHeightToParent(adjust = -70f)
                
                this += ScrollPane().apply { 
                    this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)
                    this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
                    
                    this.setContent(VBox().apply { 
                        this.spacing.set(2f)
                        this.temporarilyDisableLayouts { 
                            desktopScenario.inboxItems.mapByID.values.forEach { inboxItem ->
                                this += ActionablePane().apply {
                                    this.bounds.height.set(48f)
                                    this += RectElement().apply {
                                        this.color.bind { 
                                            if (currentInboxFolder.use() == inboxItem) {
                                                Color(0f, 1f, 1f, 0.2f)
                                            } else Color(1f, 1f, 1f, 0.2f)
                                        }
                                        this.padding.set(Insets(4f))
                                        this += TextLabel("Item: ${inboxItem.id}").apply {
                                            this.renderAlign.set(RenderAlign.topLeft)
                                        }
                                    }
                                    this.setOnAction { 
                                        if (currentInboxFolder.getOrCompute() == inboxItem) {
                                            currentInboxFolder.set(null)
                                        } else {
                                            currentInboxFolder.set(inboxItem)
                                        }
                                    }
                                }
                            }
                        }
                        this.autoSizeToChildren.set(true)
                    })
                }
            }
        }
        
        val contentScrollPane = ScrollPane().apply {
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.AS_NEEDED)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(0f, 0f, 0f, 0.5f))
            this.visible.bind { currentInboxFolder.use() != null }
        }
        columns[1] += contentScrollPane


        bg += Button("Play Contract").apply {
            Anchor.BottomCentre.configure(this)
            this.skinID.set(PRManiaSkins.BUTTON_SKIN_STORY_DARK)
            this.bounds.width.set(300f)
            this.bounds.height.set(40f)
            this.visible.bind { currentInboxFolder.use() is InboxItem.ContractDoc }
            this.setOnAction {
                val newItem = currentInboxFolder.getOrCompute()
                if (newItem is InboxItem.ContractDoc) {
                    storySession.musicHandler.fadeOut(0f)
                    val contract = newItem.contract
                    main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_enter_game"))
                    val gameMode = contract.gamemodeFactory(main)
                    val playScreen = StoryPlayScreen(main, storySession, gameMode.container, Challenges.NO_CHANGES,
                            main.settings.inputCalibration.getOrCompute(), gameMode, contract, true, 0, this@TestStoryAllInboxItemsScreen) {
                        Paintbox.LOGGER.debug("ExitReason: $it")
                    }
                    if (Gdx.input.isShiftDown()) {
                        gameMode.engine.autoInputs = true
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
        
        currentInboxFolder.addListener { varr ->
            val newItem = varr.getOrCompute()
            if (newItem != null) {
                val vbox = VBox().apply { 
                    this.spacing.set(8f)
                    this.temporarilyDisableLayouts {
                        this += inboxItemRenderer.createInboxItemUI(newItem).apply {
                            Anchor.TopCentre.xConfigure(this, 0f)
                        }
                    }
                }
                
                contentScrollPane.setContent(vbox)
                vbox.sizeHeightToChildren()
            } else {
                contentScrollPane.setContent(Pane())
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