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
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.Markup
import paintbox.font.TextRun
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
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.inbox.InboxDB
import polyrhythmmania.storymode.inbox.InboxItem
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

    private val slabMarkup: Markup = Markup(mapOf(Markup.FONT_NAME_BOLD to main.fontRobotoSlabBold), TextRun(main.fontRobotoSlab, ""), Markup.FontStyles(Markup.FONT_NAME_BOLD, Markup.DEFAULT_FONT_NAME, Markup.DEFAULT_FONT_NAME))
    
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
                            InboxDB.allItems.values.forEach { inboxItem ->
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
            return when (item) {
                is InboxItem.Memo -> {
                    RectElement(Color.WHITE).apply {
                        this.doClipping.set(true)
                        this.border.set(Insets(2f))
                        this.borderStyle.set(SolidBorder(Color.valueOf("7FC9FF")))
                        this.bounds.height.set(600f)
                        this.bindWidthToSelfHeight(multiplier = 1f / sqrt(2f))
                        
                        this.padding.set(Insets(16f))
                        
                        this += VBox().apply { 
                            this.spacing.set(6f)
                            this.temporarilyDisableLayouts {
                                this += TextLabel(StoryL10N.getVar("inboxItem.memo.title"), font = main.fontMainMenuHeading).apply {
                                    this.bounds.height.set(40f)
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(Align.topLeft)
                                    this.padding.set(Insets(0f, 8f, 0f, 8f))
                                }
                                this += ColumnarPane(3, true).apply {
                                    this.bounds.height.set(32f * 3)

                                    fun addField(index: Int, type: String, valueField: String, valueMarkup: Markup? = null) {
                                        this[index] += Pane().apply {
                                            this.margin.set(Insets(2f))
                                            this += TextLabel(StoryL10N.getVar("inboxItem.memo.${type}"), font = main.fontRobotoBold).apply {
                                                this.textColor.set(Color.BLACK)
                                                this.renderAlign.set(Align.left)
                                                this.padding.set(Insets(2f, 2f, 0f, 10f))
                                                this.bounds.width.set(90f)
                                            }
                                            this += TextLabel(valueField, font = main.fontRobotoSlab).apply {
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
                                    
                                    addField(0, "to", StoryL10N.getValue("inboxItemDetails.${item.id}.to"))
                                    addField(1, "from", StoryL10N.getValue("inboxItemDetails.${item.id}.from"))
                                    addField(2, "subject", StoryL10N.getValue("inboxItemDetails.${item.id}.subject"))
                                }
                                this += RectElement(Color.BLACK).apply { 
                                    this.bounds.height.set(2f)
                                }

                                this += TextLabel(StoryL10N.getValue("inboxItemDetails.${item.id}.desc")).apply {
                                    this.markup.set(slabMarkup)
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(Align.topLeft)
                                    this.padding.set(Insets(8f, 0f, 0f, 0f))
                                    this.bounds.height.set(400f)
                                    this.doLineWrapping.set(true)
                                }
                            }
                        }
                    }
                }
                is InboxItem.ContractDoc -> {
                    RectElement(Color.WHITE).apply {
                        this.doClipping.set(true)
                        this.border.set(Insets(2f))
                        this.borderStyle.set(SolidBorder(Color.valueOf("7FC9FF")))
                        this.bounds.height.set(600f)
                        this.bindWidthToSelfHeight(multiplier = 1f / sqrt(2f))

                        this.padding.set(Insets(16f))

                        this += VBox().apply {
                            this.spacing.set(6f)
                            this.temporarilyDisableLayouts {
                                this += TextLabel(ReadOnlyVar.const("<Letterhead>"), font = main.fontMainMenuHeading).apply {
                                    this.bounds.height.set(40f)
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(Align.top)
                                    this.padding.set(Insets(0f, 8f, 0f, 8f))
                                }
                                this += RectElement(Color.BLACK).apply {
                                    this.bounds.height.set(2f)
                                }
                                this += ColumnarPane(3, true).apply {
                                    this.bounds.height.set(28f * 3)
                                    
                                    fun addField(index: Int, type: String, valueField: String, valueMarkup: Markup? = null) {
                                        this[index] += Pane().apply {
                                            this.margin.set(Insets(2f))
                                            this += TextLabel(StoryL10N.getVar("inboxItem.contract.${type}"), font = main.fontRobotoBold).apply {
                                                this.textColor.set(Color.BLACK)
                                                this.renderAlign.set(Align.right)
                                                this.padding.set(Insets(2f, 2f, 0f, 4f))
                                                this.bounds.width.set(96f)
                                            }
                                            this += TextLabel(valueField, font = main.fontRobotoSlab).apply {
                                                this.textColor.set(Color.BLACK)
                                                this.renderAlign.set(Align.left)
                                                this.padding.set(Insets(2f, 2f, 4f, 0f))
                                                this.bounds.x.set(96f)
                                                this.bindWidthToParent(adjust = -96f)
                                                if (valueMarkup != null) {
                                                    this.markup.set(valueMarkup)
                                                }
                                            }
                                        }
                                    }
                                    
                                    addField(0, "title", item.contract.name.getOrCompute())
                                    addField(1, "requester", item.contract.requester.localizedName.getOrCompute())
                                    this[2] += Pane().apply {
                                        this.margin.set(Insets(2f))
                                        this += TextLabel(item.contract.tagline.getOrCompute(), font = main.fontRobotoSlab).apply {
                                            this.textColor.set(Color.BLACK)
                                            this.renderAlign.set(Align.center)
                                            this.padding.set(Insets(2f, 2f, 4f, 0f))
                                        }
                                    }
                                }
                                this += RectElement(Color.BLACK).apply {
                                    this.bounds.height.set(2f)
                                }

                                this += TextLabel(StoryL10N.getValue("contract.desc.${item.id}")).apply {
                                    this.markup.set(slabMarkup)
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(Align.topLeft)
                                    this.padding.set(Insets(8f, 4f, 0f, 0f))
                                    this.bounds.height.set(400f)
                                    this.doLineWrapping.set(true)
                                    this.autosizeBehavior.set(TextLabel.AutosizeBehavior.Active(TextLabel.AutosizeBehavior.Dimensions.HEIGHT_ONLY))
                                }

                                if (item.contract.conditions.isNotEmpty()) {
                                    this += TextLabel(StoryL10N.getValue("inboxItem.contract.conditions"), font = main.fontRobotoBold).apply {
                                        this.textColor.set(Color.BLACK)
                                        this.renderAlign.set(Align.bottomLeft)
                                        this.padding.set(Insets(0f, 6f, 4f, 0f))
                                        this.bounds.height.set(32f)
                                    }
                                    item.contract.conditions.forEach { condition ->
                                        this += TextLabel("• " + condition.name.getOrCompute(), font = main.fontRobotoSlab).apply {
                                            this.textColor.set(Color.BLACK)
                                            this.renderAlign.set(Align.left)
                                            this.padding.set(Insets(2f, 2f, 16f, 0f))
                                            this.bounds.height.set(20f)
                                        }
                                    }
                                }
                                
                                this += RectElement(Color(0f, 0f, 0f, 0.75f)).apply { 
                                    this.bounds.height.set(48f)
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