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
import polyrhythmmania.storymode.inbox.InboxState
import polyrhythmmania.storymode.inbox.progression.Progression
import polyrhythmmania.storymode.inbox.progression.StoryModeProgression
import polyrhythmmania.storymode.inbox.progression.UnlockStage
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
                            if (stage.minRequiredToComplete < stage.requiredInboxItems.size) {
                                so.add("minRequiredToComplete", stage.minRequiredToComplete)
                            }
                        })
                    }
                })
            }
        }

        private fun loadProgressionJson(json: JsonObject, inboxState: InboxState): Progression {
            val array = json["list"].asArray()
            return Progression(array.mapIndexed { index, value ->
                val obj = value.asObject()
                val requiredInboxItems = obj["required"].asArray().map { it.asString() }
                UnlockStage("stage_$index", { true }, requiredInboxItems,
                        obj["optional"]?.asArray()?.map { it.asString() } ?: emptyList(),
                        obj["minRequiredToComplete"]?.asInt() ?: requiredInboxItems.size)
            }).apply { 
                this.checkAll(inboxState)
            }
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
    private val progression: Var<Progression> = Var(StoryModeProgression())

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
                this.sizeHeightToChildren(100f)
                scrollPane.setContent(Pane().apply { this.bounds.height.set(1f) })
                scrollPane.vBar.setValue(0f)
                scrollPane.setContent(this)
            }
            reload(progression.getOrCompute())
            progression.addListener {
                reload(it.getOrCompute())
            }
        }
        scrollPane.setContent(vbox)
        pane += scrollPane


        bg += VBox().apply {
            this.bounds.width.set(120f)
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
                        Gdx.app.clipboard.contents = StoryModeProgression().toJson().toString(WriterConfig.PRETTY_PRINT)
                        text.set("Copied to\nclipboard!")
                        this@TestStoryProgressionOverviewScreen.sceneRoot.animations.enqueueAnimation(Animation(Interpolation.linear, 5f, 0f, 1f).apply {
                            this.onComplete = {
                                text.set(normalText)
                                timer.set(0f)
                            }
                        }, timer)
                    }
                }
                this += Button("").apply {
                    this.bounds.height.set(72f)
                    this.markup.set(main.debugMarkup)

                    val normalText = "Reset to\ndefault\nprogression"
                    this.text.set(normalText)

                    val timer = FloatVar(0f)
                    this.setOnAction {
                        text.set("Done!")
                        this@TestStoryProgressionOverviewScreen.progression.set(StoryModeProgression())
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
                        fun triggerResetTimer() {
                            this@TestStoryProgressionOverviewScreen.sceneRoot.animations.enqueueAnimation(Animation(Interpolation.linear, 5f, 0f, 1f).apply {
                                this.onComplete = {
                                    text.set(normalText)
                                    timer.set(0f)
                                }
                            }, timer)
                        }

                        try {
                            if (!Gdx.app.clipboard.hasContents()) {
                                text.set("No data in\nclipboard.\nTry again")
                            } else {
                                val contents: String? = Gdx.app.clipboard.contents
                                if (contents == null) {
                                    text.set("No data in\nclipboard.\nTry again")
                                } else {
                                    val json = Json.parse(contents).asObject()
                                    val progression = loadProgressionJson(json, inboxState)

                                    // Check progression for valid IDs
                                    if (progression.stages.isEmpty()) {
                                        text.set("ERR: No stages\nwere defined.")
                                    } else {
                                        var successful = true
                                        outer@ for ((stageIndex, stage) in progression.stages.withIndex()) {
                                            for ((type, list) in listOf("required" to stage.requiredInboxItems, "optional" to stage.optionalInboxItems)) {
                                                for ((idIndex, id) in list.withIndex()) {
                                                    val contractDoc = getContractDoc(id)
                                                    if (contractDoc == null && InboxDB.mapByID[id] == null) {
                                                        text.set("ERR: Unknown ID\n\"${id}\"\nin stage #${stageIndex}\n${type} #${idIndex}")
                                                        successful = false
                                                        break@outer
                                                    }
                                                }
                                            }
                                        }

                                        if (successful) {
                                            this@TestStoryProgressionOverviewScreen.progression.set(progression)
                                            text.set("Loaded\nsuccessfully!")
                                        }
                                    }
                                }
                            }

                            triggerResetTimer()
                        } catch (e: Exception) {
                            e.printStackTrace()

                            text.set("Error when\nreading JSON.\nCheck syntax!")
                            vbox.removeAllChildren()

                            triggerResetTimer()
                        }
                    }
                }
            }
        }
    }

    private fun getContractDoc(id: String): InboxItem.ContractDoc? {
        return (InboxDB.mapByID["debugcontr_$id"] as? InboxItem.ContractDoc)
                ?: (InboxDB.mapByID["contract_$id"] as? InboxItem.ContractDoc)
                ?: (InboxDB.mapByID[id] as? InboxItem.ContractDoc)
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