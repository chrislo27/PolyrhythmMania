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
import paintbox.binding.FloatVar
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
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.ColumnarPane
import paintbox.ui.layout.HBox
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.inbox.InboxDB
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.screen.StoryPlayScreen
import polyrhythmmania.ui.PRManiaSkins
import kotlin.math.sqrt


class TestStoryContractsScreen(main: PRManiaGame, val prevScreen: Screen)
    : PRManiaScreen(main) {
    
    val batch: SpriteBatch = main.batch
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val processor: InputProcessor = sceneRoot.inputSystem

    private val monoMarkup: Markup = Markup(mapOf(Markup.FONT_NAME_BOLD to main.robotoMonoFontBold, Markup.FONT_NAME_ITALIC to main.robotoMonoFontItalic, Markup.FONT_NAME_BOLDITALIC to main.robotoMonoFontBoldItalic), TextRun(main.robotoMonoFont, ""), Markup.FontStyles.ALL_USING_BOLD_ITALIC)
    private val slabMarkup: Markup = Markup(mapOf(Markup.FONT_NAME_BOLD to main.fontSlabBold), TextRun(main.fontSlab, ""), Markup.FontStyles(Markup.FONT_NAME_BOLD, Markup.DEFAULT_FONT_NAME, Markup.DEFAULT_FONT_NAME))
    
    init {
        val bg = RectElement(PRManiaColors.debugColor).apply {
            this += Button(Localization.getVar("common.back")).apply {
                this.bounds.width.set(70f)
                this.bounds.height.set(32f)
                Anchor.TopLeft.configure(this, offsetX = 8f, offsetY = 8f)
                this.setOnAction { 
                    main.screen = prevScreen
                }
            }
        }
        sceneRoot += bg

        val pane = HBox().apply {
            Anchor.BottomLeft.configure(this, offsetX = 16f, offsetY = -20f)
            this.bounds.width.set(1280f - 16f * 2)
            this.bounds.height.set(600f)
            this.spacing.set(24f)
        }
        bg += pane
        
        val columns = ColumnarPane(listOf(72, 250), false).apply {
            Anchor.TopLeft.configure(this)
            this.bounds.width.set(380f)
        }
        pane += columns
        
        columns[0] += RectElement(Color(0f, 0f, 0f, 0.5f)).apply { 
            this.bindHeightToParent(multiplier = 0.4f)
            // TODO
        }
        val currentInboxFolder: Var<InboxItem?> = Var(null)
        columns[1] += RectElement(Color(0f, 0f, 0f, 0.5f)).apply {  
            this += TextLabel(StoryL10N.getVar("desk.inbox"), font = main.fontMainMenuHeading).apply {
                this.bounds.height.set(64f)
                this.bindVarToSelfHeight(this.bounds.y, multiplier = -1f)
                this.textColor.set(Color.WHITE)
                this.renderAlign.set(Align.bottomLeft)
                this.bgPadding.set(Insets(18f, 18f, 16f, 32f))
                this.renderBackground.set(true)
                this.backgroundColor.set(Color(0f, 0f, 0f, 0.5f))
            }
            this += Pane().apply { 
//                this.bounds.y.set(70f)
//                this.bindHeightToParent(adjust = -70f)
                
                this += ScrollPane().apply { 
                    this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.ALWAYS)
                    this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
                    this.hBar.skinID.set(PRManiaSkins.SCROLLBAR_SKIN_LIGHT)
                    this.vBar.skinID.set(PRManiaSkins.SCROLLBAR_SKIN_LIGHT)
                    
                    this.setContent(VBox().apply { 
                        this.spacing.set(2f)
                        this.temporarilyDisableLayouts { 
                            InboxDB.allItems.values.sortedBy { it.fpPrereq }.forEach { inboxItem ->
                                this += ActionablePane().apply {
                                    this.bounds.height.set(48f)
                                    this += RectElement().apply {
                                        this.color.bind { 
                                            if (currentInboxFolder.use() == inboxItem) {
                                                Color(0f, 1f, 1f, 0.2f)
                                            } else Color(1f, 1f, 1f, 0.2f)
                                        }
                                        this.padding.set(Insets(4f))
                                        this += TextLabel("Item: ${inboxItem.id}", font = main.fontMainMenuMain).apply {
                                            this.renderAlign.set(RenderAlign.topLeft)
                                            this.setScaleXY(0.85f)
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
        
        val itemPane = Pane().apply { 
            this.bounds.width.set(550f)
        }
        pane += itemPane
        
        var currentItemUIElement: UIElement? = null
        currentInboxFolder.addListener { varr ->
            val lastCurrentEle = currentItemUIElement
            if (lastCurrentEle != null) {
                itemPane.removeChild(lastCurrentEle)
                currentItemUIElement = null
            }
            
            val newItem = varr.getOrCompute()
            if (newItem != null) {
                val newEle = createInboxItemUI(newItem).apply { 
                    Anchor.Centre.configure(this)
                }
                
                currentItemUIElement = newEle
                itemPane.addChild(newEle)
            }
        }
    }

    private fun createInboxItemUI(item: InboxItem): UIElement {
        return when (item) {
            is InboxItem.IndexCard -> {
                RectElement(Color.valueOf("FFF9EE")).apply {
                    this.doClipping.set(true)
                    this.border.set(Insets(1f))
                    this.borderStyle.set(SolidBorder(Color.valueOf("E5D58B")))
                    this.bindHeightToSelfWidth(multiplier = 3f / 5f)

                    // Rules, 1 main red one and 10 light blue ones
                    val spaces = (1 + 10) + 2
                    val spacing = 1f / spaces
                    val red = Color(1f, 0f, 0f, 0.5f)
                    val blue = Color(0.2f, 0.7f, 1f, 0.5f)
                    for (i in 0 until (1 + 10)) {
                        this += RectElement(if (i == 0) red else blue).apply {
                            this.bounds.height.set(1.5f)
                            this.bounds.y.bind { (parent.use()?.contentZone?.height?.use() ?: 0f) * (spacing * (i + 2)) }
                        }
                    }
                    val leftRule = FloatVar { (parent.use()?.contentZone?.width?.use() ?: 0f) * 0.125f }
                    this += RectElement(red).apply {
                        this.bounds.width.set(1.5f)
                        this.bounds.x.bind { leftRule.use() }
                    }

                    this += Pane().apply {
                        Anchor.BottomRight.configure(this)
                        this.bindHeightToParent(multiplier = 1f - spacing * 2, adjust = -5f)
                        this.bindWidthToParent(adjustBinding = { -leftRule.use() })
                        this.padding.set(Insets(0f, 0f, 3f, 15f))

                        this += TextLabel(StoryL10N.getValue("inboxItemDetails.${item.id}.desc")).apply {
                            this.markup.set(Markup(mapOf(Markup.DEFAULT_FONT_NAME to main.fontHandwriting2),
                                    TextRun(main.fontHandwriting2, "", lineHeightScale = 0.8f),
                                    styles = Markup.FontStyles.ALL_USING_DEFAULT_FONT))
                            this.renderAlign.set(RenderAlign.topLeft)
                            this.doLineWrapping.set(true)
                            this.bounds.y.set(-1f)
                        }
                    }
                }
            }
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
                                        this += TextLabel(StoryL10N.getVar("inboxItem.memo.${type}"), font = main.robotoFontBold).apply {
                                            this.textColor.set(Color.BLACK)
                                            this.renderAlign.set(Align.left)
                                            this.padding.set(Insets(2f, 2f, 0f, 10f))
                                            this.bounds.width.set(90f)
                                        }
                                        this += TextLabel(valueField, font = main.fontSlab).apply {
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
                                        this += TextLabel(StoryL10N.getVar("inboxItem.contract.${type}"), font = main.robotoFontBold).apply {
                                            this.textColor.set(Color.BLACK)
                                            this.renderAlign.set(Align.right)
                                            this.padding.set(Insets(2f, 2f, 0f, 4f))
                                            this.bounds.width.set(96f)
                                        }
                                        this += TextLabel(valueField, font = main.fontSlab).apply {
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
                                    this += TextLabel(item.contract.tagline.getOrCompute(), font = main.fontSlab).apply {
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
                                this += TextLabel(StoryL10N.getValue("inboxItem.contract.conditions"), font = main.robotoFontBold).apply {
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(Align.bottomLeft)
                                    this.padding.set(Insets(0f, 6f, 4f, 0f))
                                    this.bounds.height.set(32f)
                                }
                                item.contract.conditions.forEach { condition ->
                                    this += TextLabel("• " + condition.name.getOrCompute(), font = main.fontSlab).apply {
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
                                                main.settings.inputCalibration.getOrCompute(), gameMode, item.contract, this@TestStoryContractsScreen)
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