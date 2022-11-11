package polyrhythmmania.storymode.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.Paintbox
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.binding.asReadOnlyVar
import paintbox.font.Markup
import paintbox.font.PaintboxFont
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
import paintbox.ui.layout.ColumnarPane
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.contract.IHasContractTextInfo
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItemState
import polyrhythmmania.storymode.screen.StoryPlayScreen
import kotlin.math.sqrt


/**
 * Original all inbox items screen. Do not modify, just keeping as a reference.
 * (Replaced with [TestStoryDesktopScreen])
 */
class TestStoryAllInboxItemsScreen(main: PRManiaGame, val prevScreen: Screen)
    : PRManiaScreen(main) {
    
    val batch: SpriteBatch = main.batch
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val processor: InputProcessor = sceneRoot.inputSystem

    private val monoMarkup: Markup = Markup.createWithBoldItalic(main.fontRobotoMono, main.fontRobotoMonoBold, main.fontRobotoMonoItalic, main.fontRobotoMonoBoldItalic)
    private val slabMarkup: Markup = Markup.createWithBoldItalic(main.fontRobotoSlab, main.fontRobotoSlabBold, null, null)
    private val robotoCondensedMarkup: Markup = Markup.createWithBoldItalic(main.fontRobotoCondensed, main.fontRobotoCondensedBold, main.fontRobotoCondensedItalic, main.fontRobotoCondensedBoldItalic)
    private val openSansMarkup: Markup = Markup.createWithBoldItalic(main.fontOpenSans, main.fontOpenSansBold, main.fontOpenSansItalic, main.fontOpenSansBoldItalic)
    private val inboxItemTitleFont: PaintboxFont = main.fontLexendBold
    
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
                            DebugAllInboxItemsDB.mapByID.values.forEach { inboxItem ->
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


        fun createInboxItemUI(item: InboxItem): UIElement {
            class Paper(val root: ImageNode, val paperPane: Pane, val envelopePane: Pane)

            fun createPaperTemplate(textureID: String = "desk_contract_full"): Paper {
                val root = ImageNode(TextureRegion(StoryAssets.get<Texture>(textureID)), ImageRenderingMode.FULL).apply {
                    this.bounds.width.set(112f * 4)
                    this.bounds.height.set(150f * 4)
                }
                val paperPane = Pane().apply {// Paper part
                    this.bounds.height.set(102f * 4)
                    this.margin.set(Insets((2f + 4f) * 4, 0f * 4, (4f + 4f) * 4, (4f + 4f) * 4))
                }
                root += paperPane
                val envelopePane = Pane().apply {// Envelope part
                    this.margin.set(Insets(0f * 4, 6f * 4))
                    this.bounds.height.set(48f * 4)
                    this.bounds.y.set(102f * 4)
                }
                root += envelopePane

                return Paper(root, paperPane, envelopePane)
            }

            return when (item) {
                is InboxItem.Memo -> {
                    val paper = createPaperTemplate("desk_contract_paper")
                    paper.paperPane += VBox().apply {
                        this.spacing.set(1f * 4)
                        this.temporarilyDisableLayouts {
                            this += TextLabel(StoryL10N.getVar("inboxItem.memo.heading"), font = main.fontMainMenuHeading).apply {
                                this.bounds.height.set(9f * 4)
                                this.textColor.set(Color.BLACK)
                                this.renderAlign.set(Align.topLeft)
                                this.padding.set(Insets(0f, 2f * 4, 0f, 2f * 4))
                            }
                            val fields: List<Pair<String, ReadOnlyVar<String>>> = listOfNotNull(
                                    if (item.hasToField) ("to" to item.to) else null,
                                    "from" to item.from,
                                    "subject" to item.subject
                            )
                            this += ColumnarPane(fields.size, true).apply {
                                this.bounds.height.set((7f * 4) * fields.size)

                                fun addField(index: Int, fieldName: String, valueField: String, valueMarkup: Markup? = null) {
                                    this[index] += Pane().apply {
                                        this.margin.set(Insets(0.5f * 4, 0f))
                                        this += TextLabel(StoryL10N.getVar("inboxItem.memo.${fieldName}"), font = main.fontRobotoBold).apply {
                                            this.textColor.set(Color.BLACK)
                                            this.renderAlign.set(Align.left)
                                            this.padding.set(Insets(2f, 2f, 0f, 10f))
                                            this.bounds.width.set(22.5f * 4)
                                        }
                                        this += TextLabel(valueField, font = main.fontRoboto).apply {
                                            this.textColor.set(Color.BLACK)
                                            this.renderAlign.set(Align.left)
                                            this.padding.set(Insets(2f, 2f, 4f, 0f))
                                            this.bounds.x.set(90f)
                                            this.bindWidthToParent(adjust = -(22.5f * 4))
                                            if (valueMarkup != null) {
                                                this.markup.set(valueMarkup)
                                            }
                                        }
                                    }
                                }

                                fields.forEachIndexed { i, (key, value) ->
                                    addField(i, key, value.getOrCompute())
                                }
                            }
                            this += RectElement(Color.BLACK).apply {
                                this.bounds.height.set(2f)
                            }

                            this += TextLabel(item.desc.getOrCompute()).apply {
                                this.markup.set(openSansMarkup)
                                this.textColor.set(Color.BLACK)
                                this.renderAlign.set(Align.topLeft)
                                this.padding.set(Insets(3f * 4, 0f, 0f, 0f))
                                this.bounds.height.set(100f * 4)
                                this.doLineWrapping.set(true)
                            }
                        }
                    }

                    paper.root
                }
                is InboxItem.InfoMaterial -> {
                    val paper = createPaperTemplate("desk_contract_paper")
                    paper.paperPane += VBox().apply {
                        this.spacing.set(1f * 4)
                        this.temporarilyDisableLayouts {
                            this += TextLabel(StoryL10N.getVar("inboxItem.infoMaterial.heading"), font = main.fontMainMenuHeading).apply {
                                this.bounds.height.set(9f * 4)
                                this.textColor.set(Color.BLACK)
                                this.renderAlign.set(Align.top)
                                this.padding.set(Insets(0f, 2f * 4, 0f, 0f))
                            }
                            val fields: List<Pair<String, ReadOnlyVar<String>>> = listOfNotNull(
                                    "topic" to item.topic,
                                    "audience" to item.audience,
                            )
                            this += ColumnarPane(fields.size, true).apply {
                                this.bounds.height.set((7f * 4) * fields.size)

                                fun addField(index: Int, fieldName: String, valueField: String, valueMarkup: Markup? = null) {
                                    this[index] += Pane().apply {
                                        this.margin.set(Insets(0.5f * 4, 0f))
                                        this += TextLabel(StoryL10N.getVar("inboxItem.infoMaterial.${fieldName}"), font = main.fontRobotoBold).apply {
                                            this.textColor.set(Color.BLACK)
                                            this.renderAlign.set(Align.left)
                                            this.padding.set(Insets(2f, 2f, 0f, 10f))
                                            this.bounds.width.set(22.5f * 4)
                                        }
                                        this += TextLabel(valueField, font = main.fontRoboto).apply {
                                            this.textColor.set(Color.BLACK)
                                            this.renderAlign.set(Align.left)
                                            this.padding.set(Insets(2f, 2f, 4f, 0f))
                                            this.bounds.x.set(90f)
                                            this.bindWidthToParent(adjust = -(22.5f * 4))
                                            if (valueMarkup != null) {
                                                this.markup.set(valueMarkup)
                                            }
                                        }
                                    }
                                }

                                fields.forEachIndexed { i, (key, value) ->
                                    addField(i, key, value.getOrCompute())
                                }
                            }
                            this += RectElement(Color.BLACK).apply {
                                this.bounds.height.set(2f)
                            }

                            this += TextLabel(item.desc.getOrCompute()).apply {
                                this.markup.set(robotoCondensedMarkup)
                                this.textColor.set(Color.BLACK)
                                this.renderAlign.set(Align.topLeft)
                                this.padding.set(Insets(3f * 4, 0f, 0f, 0f))
                                this.bounds.height.set(100f * 4)
                                this.doLineWrapping.set(true)
                            }
                        }
                    }

                    paper.root
                }
                is InboxItem.ContractDoc, is InboxItem.PlaceholderContract -> {
                    item as IHasContractTextInfo
                    val paper = createPaperTemplate()

                    val headingText: ReadOnlyVar<String> = if (item is InboxItem.ContractDoc) {
                        item.headingText
                    } else if (item is InboxItem.PlaceholderContract) {
                        item.headingText
                    } else {
                        "<missing heading text>".asReadOnlyVar()
                    }

                    paper.paperPane += VBox().apply {
                        this.spacing.set(1f * 4)
                        this.temporarilyDisableLayouts {
                            this += Pane().apply {
                                this.bounds.height.set(12f * 4)
                                this.margin.set(Insets(0f, 2.5f * 4, 0f, 0f))

                                this += TextLabel(headingText, font = main.fontMainMenuHeading).apply {
                                    this.bindWidthToParent(multiplier = 0.5f, adjust = -2f * 4)
                                    this.padding.set(Insets(0f, 0f, 0f, 1f * 4))
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(Align.left)
                                }
                                this += Pane().apply {
                                    Anchor.TopRight.configure(this)
                                    this.bindWidthToParent(multiplier = 0.5f, adjust = -2f * 4)

                                    this += TextLabel(item.name, font = main.fontRobotoMonoBold).apply {
                                        this.textColor.set(Color.BLACK)
                                        this.renderAlign.set(Align.topRight)
                                    }
                                    this += TextLabel(item.requester.localizedName, font = main.fontRobotoCondensedItalic).apply {
                                        this.textColor.set(Color.BLACK)
                                        this.renderAlign.set(Align.bottomRight)
                                    }
                                }
                            }
                            this += RectElement(Color.BLACK).apply {
                                this.bounds.height.set(2f)
                            }
                            this += TextLabel(item.tagline.getOrCompute(), font = main.fontLexend).apply {
                                this.bounds.height.set(10f * 4)
                                this.textColor.set(Color.BLACK)
                                this.renderAlign.set(Align.center)
                                this.padding.set(Insets(1f * 4, 1f * 4, 1f * 4, 0f))
                            }
                            this += RectElement(Color.BLACK).apply {
                                this.bounds.height.set(2f)
                            }

                            this += TextLabel(item.desc.getOrCompute()).apply {
                                this.markup.set(openSansMarkup)
                                this.textColor.set(Color.BLACK)
                                this.renderAlign.set(Align.topLeft)
                                this.padding.set(Insets(8f, 4f, 0f, 0f))
                                this.bounds.height.set(400f)
                                this.doLineWrapping.set(true)
                                this.autosizeBehavior.set(TextLabel.AutosizeBehavior.Active(TextLabel.AutosizeBehavior.Dimensions.HEIGHT_ONLY))
                            }
                        }
                    }

                    if (item is InboxItem.ContractDoc) {
                        paper.envelopePane += RectElement(Color(0f, 0f, 0f, 0.75f)).apply { // FIXME this is a temp button
                            this.bounds.y.set(29f * 4)
                            this.bounds.height.set(16f * 4)
                            this.padding.set(Insets(8f))

                            this += Button("Play Level").apply {
                                this.setOnAction {
                                    main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_enter_game"))
                                    val gameMode = item.contract.gamemodeFactory(main)
                                    val playScreen = StoryPlayScreen(main, gameMode.container, Challenges.NO_CHANGES,
                                            main.settings.inputCalibration.getOrCompute(), gameMode, item.contract, this@TestStoryAllInboxItemsScreen) {
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
                    }

                    paper.root
                }
                is InboxItem.Debug -> {
                    RectElement(Color.WHITE).apply {
                        this.doClipping.set(true)
                        this.border.set(Insets(2f))
                        this.borderStyle.set(SolidBorder(Color.YELLOW))
                        this.bounds.height.set(600f)
                        this.bindWidthToSelfHeight(multiplier = 1f / sqrt(2f))

                        this.padding.set(Insets(16f))

                        this += VBox().apply {
                            this.spacing.set(6f)
                            this.temporarilyDisableLayouts {
                                this += TextLabel("DEBUG ITEM", font = main.fontMainMenuHeading).apply {
                                    this.bounds.height.set(40f)
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(Align.top)
                                    this.padding.set(Insets(0f, 8f, 0f, 8f))
                                }
                                this += RectElement(Color.BLACK).apply {
                                    this.bounds.height.set(2f)
                                }
                                this += ColumnarPane(3, true).apply {
                                    this.bounds.height.set(32f * this.numRealColumns)

                                    fun addField(index: Int, key: String, valueField: ReadOnlyVar<String>,
                                                 valueMarkup: Markup? = null) {
                                        this[index] += Pane().apply {
                                            this.margin.set(Insets(2f))
                                            this += TextLabel(key, font = main.fontRobotoBold).apply {
                                                this.textColor.set(Color.BLACK)
                                                this.renderAlign.set(Align.left)
                                                this.padding.set(Insets(2f, 2f, 0f, 10f))
                                                this.bounds.width.set(90f)
                                            }
                                            this += TextLabel(valueField, font = main.fontRoboto).apply {
                                                this.textColor.set(Color.BLACK)
                                                this.renderAlign.set(Align.left)
                                                this.padding.set(Insets(2f, 2f, 4f, 0f))
                                                this.bounds.x.set(90f)
                                                this.bindWidthToParent(adjust = -90f)
                                                if (valueMarkup != null) {
                                                    this.markup.set(valueMarkup)
                                                }
                                            }
                                        }
                                    }
                                    fun addField(index: Int, key: String, valueField: String,
                                                 valueMarkup: Markup? = null) {
                                        addField(index, key, ReadOnlyVar.const(valueField), valueMarkup)
                                    }

                                    addField(0, "Type", "${item.subtype}")
                                    addField(1, "ID", item.id)
                                    addField(2, "ItemState", Var.bind {
                                        // Always unavailable in debug screen
                                        InboxItemState.Unavailable.toString()
                                    })
                                }
                                this += RectElement(Color.BLACK).apply {
                                    this.bounds.height.set(2f)
                                }

                                this += TextLabel(item.description).apply {
                                    this.markup.set(slabMarkup)
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(Align.topLeft)
                                    this.padding.set(Insets(8f, 0f, 0f, 0f))
                                    this.bounds.height.set(150f)
                                    this.doLineWrapping.set(true)
                                }
                                
                                // Intentionally exclude debug progressoin buttons
                            }
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
                        this += createInboxItemUI(newItem).apply {
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