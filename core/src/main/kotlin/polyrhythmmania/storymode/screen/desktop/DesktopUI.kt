package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.Paintbox
import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyBooleanVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.Markup
import paintbox.font.PaintboxFont
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.*
import paintbox.ui.element.RectElement
import paintbox.ui.layout.ColumnarPane
import paintbox.ui.layout.VBox
import paintbox.util.MathHelper
import paintbox.util.gdxutils.isAltDown
import paintbox.util.gdxutils.isControlDown
import paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItemState
import polyrhythmmania.storymode.screen.StoryPlayScreen
import polyrhythmmania.storymode.test.TestStoryDesktopScreen
import polyrhythmmania.ui.PRManiaSkins
import kotlin.math.sqrt

class DesktopUI(
        val scenario: DesktopScenario,
        val rootScreen: TestStoryDesktopScreen, // TODO remove this?
) : Disposable {

    private val main: PRManiaGame = rootScreen.main
    private val batch: SpriteBatch = rootScreen.batch
    
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 320f * 4, 180f * 4) // 320x180 is the virtual resolution. Needs to be 4x (1280x720) for font scaling
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

    private val blinker: ReadOnlyBooleanVar = BooleanVar {
        MathHelper.getTriangleWave(0.75f * 2) > 0.5f
    }
    private val currentInboxItem: ReadOnlyVar<InboxItem?>
    val bg: UIElement
    
    init { // Background, base settings
        sceneRoot.debugOutlineColor.set(Color(1f, 0f, 0f, 1f))

        sceneRoot += NoInputPane().apply {
            this += ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_bg")))
            this += ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_bg_pistons")))
            this += ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_bg_pipes_lower")))
            this += ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_bg_pipes_upper")))
            
            this += object : Pane() {
                override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
                    blinker.invalidate()
                }
            }.apply { 
                this.bounds.width.set(0f)
                this.bounds.height.set(0f)
            }
        }
        bg = Pane().apply {
            this += Button(Localization.getVar("common.back")).apply {
                this.bounds.width.set(64f)
                this.bounds.height.set(32f)
                Anchor.TopLeft.configure(this, offsetX = 8f, offsetY = 8f)
                this.setOnAction {
                    main.screen = rootScreen.prevScreen
                }
            }
        }
        sceneRoot += bg
    }

    init { // Left scroll area and inbox item view
        val frameImg = ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_inboxitem_frame"))).apply {
            this.bounds.x.set(16f * 4)
            this.bounds.y.set(14f * 4)
            this.bounds.width.set(86f * 4)
            this.bounds.height.set(152f * 4)
            this.doClipping.set(true) // Safety clipping so nothing exceeds the overall frame
        }
        bg += frameImg
        val frameNoCaps = Pane().apply {// Extra pane is so that the frameChildArea is the one that clips properly internally
            this.margin.set(Insets(7f * 4, 5f * 4, 1f * 4, 1f * 4))
        }
        val frameChildArea = Pane().apply {
            this.doClipping.set(true)
            this.padding.set(Insets(0f * 4, 0f * 4, 3f * 4, 3f * 4))
        }
        frameNoCaps += frameChildArea
        frameImg += frameNoCaps
        frameImg += ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_inboxitem_frame_cap_top"))).apply {
            Anchor.TopLeft.configure(this, offsetY = 1f * 4)
            this.bounds.height.set(7f * 4)
        }
        frameImg += ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_inboxitem_frame_cap_bottom"))).apply {
            Anchor.BottomLeft.configure(this, offsetY = -1f * 4)
            this.bounds.height.set(7f * 4)
        }

        val frameScrollPane = ScrollPane().apply {
            this.contentPane.doClipping.set(false)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))
        }
        frameChildArea += frameScrollPane

        val vbar = ScrollBar(ScrollBar.Orientation.VERTICAL).apply {
            this.bounds.x.set(2f * 4)
            this.bounds.y.set(16f * 4)
            this.bounds.width.set(13f * 4)
            this.bounds.height.set(148f * 4)
            this.skinID.set(PRManiaSkins.SCROLLBAR_SKIN_STORY_DESK)

            this.minimum.set(0f)
            this.maximum.set(100f)
            this.disabled.eagerBind { frameScrollPane.vBar.maximum.use() <= 0f } // Uses the contentHeightDiff internally in ScrollPane
            this.visibleAmount.bind { (17f / 148f) * (maximum.use() - minimum.use()) } // Thumb size
            /* TODO this is hardcoded to 1/(numElements - numVisible) */
            this.blockIncrement.eagerBind { (1f / (scenario.inboxItems.items.size - 7).coerceAtLeast(1)) * (maximum.use() - minimum.use()) } // One item at a time

            // Remove up/down arrow buttons
            this.removeChild(this.increaseButton)
            this.removeChild(this.decreaseButton)
            this.thumbArea.bindWidthToParent()
            this.thumbArea.bindHeightToParent()
        }
        frameScrollPane.contentPane.contentOffsetY.eagerBind {
            -vbar.value.use() / (vbar.maximum.use() - vbar.minimum.use()) * frameScrollPane.contentHeightDiff.use()
        }
        bg += vbar
        val scrollListener = InputEventListener { event ->
            if (event is Scrolled && !Gdx.input.isControlDown() && !Gdx.input.isAltDown()) {
                val shift = Gdx.input.isShiftDown()
                val vBarAmount = if (shift) event.amountX else event.amountY

                if (vBarAmount != 0f && vbar.apparentVisibility.get() && !vbar.apparentDisabledState.get()) {
                    if (vBarAmount > 0) vbar.incrementBlock() else vbar.decrementBlock()
                }
            }
            false
        }
        vbar.addInputEventListener(scrollListener)
        frameScrollPane.addInputEventListener(scrollListener)

        val itemsVbox = VBox().apply {
            this.spacing.set(0f)
            this.margin.set(Insets(0f, 1f * 4, 0f, 0f))
            this.autoSizeToChildren.set(true)
        }
        frameScrollPane.setContent(itemsVbox)

        val itemToggleGroup = ToggleGroup()
        currentInboxItem = Var.eagerBind {
            val newToggle = itemToggleGroup.activeToggle.use() as? InboxItemTestObj
            newToggle?.inboxItem
        }
        fun addObj(obj: InboxItemTestObj) {
            itemsVbox += obj
            itemToggleGroup.addToggle(obj)
        }
        scenario.inboxItems.items.forEach { ii ->
            // TODO link InboxItem to an UnlockStage; check UnlockStage state
            addObj(InboxItemTestObj(ii))
        }


        val inboxItemDisplayPane: UIElement = VBox().apply {
            this.bounds.x.set(104f * 4)
            this.bounds.width.set(128f * 4)
            this.align.set(VBox.Align.CENTRE)
        }
        bg += inboxItemDisplayPane

        currentInboxItem.addListener {
            val inboxItem = it.getOrCompute()
            inboxItemDisplayPane.removeAllChildren()
            if (inboxItem != null) {
                inboxItemDisplayPane.addChild(createInboxItemUI(inboxItem))
            }
        }
    }
    
    init {
        // Bottom-right UI panel
    }

    private fun createInboxItemUI(item: InboxItem): UIElement {
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

                                fun addField(index: Int, type: String, valueField: String,
                                             valueMarkup: Markup? = null) {
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
                ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_contract_full")), ImageRenderingMode.FULL).apply {
                    this.bounds.width.set(112f * 4)
                    this.bounds.height.set(150f * 4)

                    this += Pane().apply {
                        this.margin.set(Insets(2f * 4, 2f * 4, (2f + 2f) * 4, (2f + 2f) * 4) + Insets(4f * 4))

                        this += VBox().apply {
                            this.spacing.set(6f)
                            this.temporarilyDisableLayouts {
                                this += TextLabel(ReadOnlyVar.const("Contract"), font = main.fontMainMenuHeading).apply {
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

                                    fun addField(index: Int, type: String, valueField: String,
                                                 valueMarkup: Markup? = null) {
                                        this[index] += Pane().apply {
                                            this.margin.set(Insets(2f))
                                            this += TextLabel(StoryL10N.getVar("inboxItem.contract.${type}"), font = main.fontRobotoBold).apply {
                                                this.textColor.set(Color.BLACK)
                                                this.renderAlign.set(Align.right)
                                                this.padding.set(Insets(2f, 2f, 0f, 4f))
                                                this.bounds.width.set(96f)
                                            }
                                            this += TextLabel(valueField, font = main.fontRobotoCondensed).apply {
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
                                        this += TextLabel(item.contract.tagline.getOrCompute(), font = main.fontLexend).apply {
                                            this.textColor.set(Color.BLACK)
                                            this.renderAlign.set(Align.center)
                                            this.padding.set(Insets(2f, 2f, 4f, 0f))
                                        }
                                    }
                                }
                                this += RectElement(Color.BLACK).apply {
                                    this.bounds.height.set(2f)
                                }

                                this += TextLabel(item.contract.desc.getOrCompute()).apply {
                                    this.markup.set(openSansMarkup)
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(Align.topLeft)
                                    this.padding.set(Insets(8f, 4f, 0f, 0f))
                                    this.bounds.height.set(400f)
                                    this.doLineWrapping.set(true)
                                    this.autosizeBehavior.set(TextLabel.AutosizeBehavior.Active(TextLabel.AutosizeBehavior.Dimensions.HEIGHT_ONLY))
                                }

                                this += RectElement(Color(0f, 0f, 0f, 0.75f)).apply {
                                    this.bounds.height.set(48f)
                                    this.padding.set(Insets(8f))
                                    this += Button("Play Level").apply {
                                        this.setOnAction {
                                            main.playMenuSfx(AssetRegistry.get<Sound>("sfx_menu_enter_game"))
                                            val gameMode = item.contract.gamemodeFactory(main)
                                            val playScreen = StoryPlayScreen(main, gameMode.container, Challenges.NO_CHANGES,
                                                    main.settings.inputCalibration.getOrCompute(), gameMode, item.contract, rootScreen) {
                                                Paintbox.LOGGER.debug("ExitReason: $it")
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
                                val itemStateVar = scenario.inboxState.itemStateVar(item.id)
                                addField(2, "ItemState", Var.bind {
                                    (itemStateVar.use() ?: InboxItemState.Unavailable).toString()
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

                            when (item.subtype) {
                                InboxItem.Debug.DebugSubtype.PROGRESSION_ADVANCER -> {
                                    this += RectElement(Color(0f, 0f, 0f, 0.75f)).apply {
                                        this.bounds.height.set(48f)
                                        this.padding.set(Insets(8f))
                                        this += Button("Mark Available (w/ flashing)").apply {
                                            this.bindWidthToParent(multiplier = 0.5f, adjust = -2f)
                                            this.setOnAction {
                                                scenario.inboxState.putItemState(item, InboxItemState.Available(true))
                                                scenario.updateProgression()
                                                scenario.updateInboxItemAvailability()
                                            }
                                        }
                                        this += Button("Mark Available (w/o flashing)").apply {
                                            this.bindWidthToParent(multiplier = 0.5f, adjust = -2f)
                                            Anchor.TopRight.configure(this)
                                            this.setOnAction {
                                                scenario.inboxState.putItemState(item, InboxItemState.Available(false))
                                                scenario.updateProgression()
                                                scenario.updateInboxItemAvailability()
                                            }
                                        }
                                    }
                                    this += RectElement(Color(0f, 0f, 0f, 0.75f)).apply {
                                        this.bounds.height.set(48f)
                                        this.padding.set(Insets(8f))
                                        this += Button("Mark Skipped").apply {
                                            this.setOnAction {
                                                scenario.inboxState.putItemState(item, InboxItemState.Skipped)
                                                scenario.updateProgression()
                                                scenario.updateInboxItemAvailability()
                                            }
                                        }
                                    }
                                    this += RectElement(Color(0f, 0f, 0f, 0.75f)).apply {
                                        this.bounds.height.set(48f)
                                        this.padding.set(Insets(8f))
                                        this += Button("Mark Completed").apply {
                                            this.setOnAction {
                                                scenario.inboxState.putItemState(item, InboxItemState.Completed(null))
                                                scenario.updateProgression()
                                                scenario.updateInboxItemAvailability()
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


    fun onResize(width: Int, height: Int) {
        uiViewport.update(width, height)
    }

    fun enableInputs() {
        main.inputMultiplexer.removeProcessor(processor)
        main.inputMultiplexer.addProcessor(processor)
    }

    fun disableInputs() {
        main.inputMultiplexer.removeProcessor(processor)
    }

    override fun dispose() {
    }

    inner class InboxItemTestObj(val inboxItem: InboxItem) : ActionablePane(), Toggle {
        override val selectedState: BooleanVar = BooleanVar(false)
        override val toggleGroup: Var<ToggleGroup?> = Var(null)
        
        private val currentInboxItemState: ReadOnlyVar<InboxItemState> = scenario.inboxState.itemStateVarOrUnavailable(inboxItem.id)

        init {
            this.bounds.width.set(78f * 4)
            this.bounds.height.set(20f * 4)

            val contentPane = Pane().apply {
                this.margin.set(Insets(1f * 4, 0f * 4, 1f * 4, 1f * 4))
            }
            this += contentPane

            contentPane += ImageNode().apply { 
                this.textureRegion.sideEffecting(TextureRegion()) {reg ->
                    val state = currentInboxItemState.use()
                    reg!!.setRegion(StoryAssets.get<Texture>("desk_inboxitem_${
                        when (state) {
                            is InboxItemState.Unavailable -> "unavailable"
                            is InboxItemState.Available -> if (!state.newIndicator || blinker.use()) "available" else "off"
                            is InboxItemState.Completed -> "cleared"
                            is InboxItemState.Skipped -> "skipped"
                        }
                    }"))
                    reg
                }
            }
            val titleAreaPane = Pane().apply {
                this.bounds.x.set((1f + 2) * 4)
                this.bounds.y.set(1f * 4)
                this.bounds.width.set(62f * 4)
                this.bounds.height.set(11f * 4)
                this.padding.set(Insets(1f * 4))
            }
            contentPane += titleAreaPane
            val bottomAreaPane = Pane().apply {
                this.bounds.x.set(3f * 4)
                this.bounds.y.set(13f * 4)
                this.bounds.width.set(62f * 4)
                this.bounds.height.set(5f * 4)
//                this.padding.set(Insets(0f * 4, 0f * 4, 1f * 4, 1f * 4))
            }
            contentPane += bottomAreaPane

            titleAreaPane += TextLabel(inboxItem.listingName, font = inboxItemTitleFont).apply {
                this.font.bind { 
                    if (currentInboxItemState.use() == InboxItemState.Unavailable) main.fontFlowCircular else inboxItemTitleFont
                }
            }

            // Selector outline/ring
            this += ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_inboxitem_selected"))).apply {
                this.bounds.x.set(-3f * 4)
                this.bounds.y.set(-1f * 4)
                this.bounds.width.set(84f * 4)
                this.bounds.height.set(23f * 4)
                this.visible.bind { selectedState.use() }
            }

            this.setOnAction {
                val currentState = currentInboxItemState.getOrCompute()
                if (currentState != InboxItemState.Unavailable) {
                    this.selectedState.invert()
                    if (currentState is InboxItemState.Available && currentState.newIndicator) {
                        scenario.inboxState.putItemState(inboxItem, currentState.copy(newIndicator = false))
                    }
                }
            }
        }
    }
}