package polyrhythmmania.storymode.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import com.eclipsesource.json.WriterConfig
import paintbox.Paintbox
import paintbox.binding.FloatVar
import paintbox.binding.Var
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.ui.*
import paintbox.ui.animation.Animation
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
    
    companion object {
        private fun Progression.toJson(): JsonObject {
            return Json.`object`().also { o ->
                o.add("list", Json.array().also { arr ->
                    this.stages.forEach { stage ->
                        arr.add(Json.`object`().also { so ->
                            so.add("required", Json.array().also { a -> 
                                stage.requiredInboxItems.forEach { a.add(it) }
                            })
                            if (stage.optionalInboxItems.isNotEmpty()) {
                                so.add("optional", Json.array().also { a ->
                                    stage.optionalInboxItems.forEach { a.add(it) }
                                })
                            }
                        })
                    }
                })
            }
        }
        
        private fun loadProgressionJson(json: JsonObject): Progression {
            TODO()
        }
    }
    
    val batch: SpriteBatch = main.batch
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val processor: InputProcessor = sceneRoot.inputSystem

    private val inboxState: InboxState = InboxState()
    private val progression: Var<Progression> = Var(StoryModeProgression(inboxState))
    
    init {
        val bg = RectElement(PRManiaColors.debugColor).apply {
            this.padding.set(Insets(8f))
        }
        sceneRoot += bg
        
        val title = TextLabel("Progression Overview (debug)", font = main.fontMainMenuHeading).apply { 
            this.bounds.width.set(600f)
            this.bounds.height.set(60f)
            Anchor.TopCentre.configure(this)
            this.renderAlign.set(RenderAlign.center)
        }
        bg += title

        val pane = Pane().apply {
            Anchor.BottomCentre.configure(this)
            this.bounds.width.set(1000f)
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

                                            val item = getContractDoc(id)
                                            if (item == null) {
                                                this.disabled.set(true)
                                                this.tooltipElement.set(Tooltip("This is not a level, but another inbox item"))
                                            } else {
                                                this.tooltipElement.set(Tooltip("Inbox Item ID: ${item.id}\nContract ID: ${item.contract.id}"))
                                            }
                                            this.setOnAction {
                                                if (item != null) {
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
                
                fun reload(progression: Progression) {
                    this.removeAllChildren()
                    this.temporarilyDisableLayouts {
                        this += createHBox(TextLabel("RED are required levels/inbox items to pass, BLUE is optional").apply {
                            this.bounds.width.set(500f)
                            this.bounds.height.set(32f)
                            this.renderAlign.set(RenderAlign.center)
                        })
                        progression.stages.flatMapIndexed { index, unlockStage ->
                            listOf(
                                    createHBox(createStageBox(unlockStage)),
                                    createHBox(createLine())
                            )
                        }.dropLast(1).forEach { this += it }
                    }
                }
                reload(progression.getOrCompute())
                progression.addListener {
                    reload(it.getOrCompute())
                }
            }
            this.sizeHeightToChildren()
        }
        scrollPane.setContent(vbox)
        pane += scrollPane


        bg += VBox().apply {
            this.bounds.width.set(100f)
            this.spacing.set(8f)
            this.temporarilyDisableLayouts {
                this += Button("Back").apply {
                    this.bounds.height.set(32f)
                    this.setOnAction {
                        main.screen = prevScreen
                    }
                }
                this += RectElement(Color.BLACK).apply { this.bounds.height.set(2f) }
                this += Button("").apply {
                    this.bounds.height.set(72f)
                    this.markup.set(main.debugMarkup)
                    
                    val normalText = "Copy DEFAULT\nJSON to\nclipboard"
                    this.text.set(normalText)
                    
                    val timer = FloatVar(0f)
                    this.setOnAction {
                        Gdx.app.clipboard.contents = StoryModeProgression(InboxState()).toJson().toString(WriterConfig.PRETTY_PRINT)
                        text.set("Copied to\nclipboard!")
                        this@TestStoryProgressionOverviewScreen.sceneRoot.animations.enqueueAnimation(Animation(Interpolation.linear, 5f, 0f, 1f).apply { 
                            this.onComplete = {
                                text.set(normalText)
                                timer.set(0f)
                            }
                        }, timer)
                    }
                }
                this += RectElement(Color.BLACK).apply { this.bounds.height.set(2f) }
                this += Button("").apply {
                    this.bounds.height.set(96f)
                    this.markup.set(main.debugMarkup)
                    
                    val normalText = "Load JSON\ndata from\nclipboard"
                    this.text.set(normalText)
                    
                    val timer = FloatVar(0f)
                    this.setOnAction {
                        // TODO
                    }
                }
            }
        }
    }
    
    private fun getContractDoc(id: String): InboxItem.ContractDoc? {
        return (InboxDB.allItems["debugcontr_$id"] as? InboxItem.ContractDoc) ?: (InboxDB.allItems["contract_$id"] as? InboxItem.ContractDoc) ?: (InboxDB.allItems[id] as? InboxItem.ContractDoc)
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