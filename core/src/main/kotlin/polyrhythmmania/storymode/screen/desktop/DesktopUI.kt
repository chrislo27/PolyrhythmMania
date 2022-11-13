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
import paintbox.binding.*
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
import paintbox.util.gdxutils.grey
import paintbox.util.gdxutils.isAltDown
import paintbox.util.gdxutils.isControlDown
import paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.inbox.IContractDoc
import polyrhythmmania.storymode.inbox.IContractDoc.ContractSubtype
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

    private val availableBlinkTexRegs: List<TextureRegion> = run {
        val numFrames = 5
        (0 until numFrames).map { i -> TextureRegion(StoryAssets.get<Texture>("desk_inboxitem_available_blink_$i")) }
    }
    private val blinkFrameIndex: ReadOnlyIntVar = IntVar {
        val secPerFrame = 0.15f
        val frames = availableBlinkTexRegs.size
        (MathHelper.getTriangleWave(secPerFrame * frames * 2) * frames).toInt().coerceIn(0, frames - 1)
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
                    blinkFrameIndex.invalidate()
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
            val newToggle = itemToggleGroup.activeToggle.use() as? InboxItemListingObj
            newToggle?.inboxItem
        }
        fun addObj(obj: InboxItemListingObj) {
            itemsVbox += obj
            itemToggleGroup.addToggle(obj)
        }
        scenario.inboxItems.items.forEach { ii ->
            // TODO link InboxItem to an UnlockStage; check UnlockStage state
            addObj(InboxItemListingObj(ii))
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
        // TODO Bottom-right UI panel
        val globalVbox = VBox().apply {
            this.bounds.x.set(240f * 4)
            this.bounds.width.set(72f * 4)
            this.margin.set(Insets(15f * 4, 0f))
            this.align.set(VBox.Align.BOTTOM)
            this.bottomToTop.set(true)
            this.spacing.set(4f * 4)
        }
        bg += globalVbox
        
        fun addPanel(title: ReadOnlyVar<String>, height: Float): VBox {
            val panel = RectElement(Color().grey(0f, 0.85f)).apply {
                Anchor.Centre.configure(this)
                this.border.set(Insets(2f * 4))
                this.borderStyle.set(SolidBorder(Color.WHITE).apply {
                    this.roundedCorners.set(true)
                })
                this.bounds.height.set(height)
                this.padding.set(Insets(4f * 4))
            }
            val vbox = VBox().apply {
                this.spacing.set(1f * 4)
                this.temporarilyDisableLayouts {
                    this += TextLabel(title, font = main.fontMainMenuHeading).apply {
                        this.bounds.height.set(8f * 4)
                        this.textColor.set(Color.WHITE)
                        this.renderAlign.set(RenderAlign.center)
                        this.margin.set(Insets(1f, 1f, 4f, 4f))
                        this.setScaleXY(0.75f)
                    }
                }
            }
            panel += vbox
            globalVbox += panel
            return vbox
        }
        
        
        fun updateForInboxItem(inboxItem: InboxItem) {
            when (inboxItem) {
                is InboxItem.ContractDoc ->  {
                    val contract = inboxItem.contract
                    val attribution = contract.attribution
                    
                    addPanel("Contract".asReadOnlyVar(), 48f * 4).apply {
                        this.temporarilyDisableLayouts {
                            this += TextLabel("High score info", font = main.fontRoboto).apply {
                                this.bounds.height.set(8f * 4)
                                this.textColor.set(Color.WHITE)
                                this.renderAlign.set(RenderAlign.center)
                                this.margin.set(Insets(1f, 1f, 4f, 4f))
                            }
                            this += Button("Start Contract", font = main.fontRoboto).apply {
                                this.bounds.height.set(8f * 4)
                            }
                        }
                    }
                    
                    if (attribution != null) {
                        val songInfo = attribution.song
                        if (songInfo != null) {
                            addPanel("Music Info".asReadOnlyVar(), (36f + (if ('\n' in songInfo.songNameAndSource.songNameWithLineBreaks) 6 else 0)) * 4).apply {
                                this.temporarilyDisableLayouts {
                                    val markupNormal = Markup.createWithBoldItalic(main.fontRoboto, main.fontRobotoBold,
                                            main.fontRobotoItalic, main.fontRobotoBoldItalic,
                                            additionalMappings = mapOf("rodin" to main.fontMainMenuRodin), lenientMode = false)
                                    val markupCondensed = Markup.createWithBoldItalic(main.fontRobotoCondensed, main.fontRobotoCondensedBold,
                                            main.fontRobotoCondensedItalic, main.fontRobotoCondensedBoldItalic,
                                            additionalMappings = mapOf("rodin" to main.fontMainMenuRodin), lenientMode = false)
                                    fun parseNonlatin(builder: Markup.Builder, text: String) {
                                        if (text.isEmpty()) return
                                        
                                        fun Char.isLatin() = this in 0.toChar()..127.toChar()

                                        var currentlyLatin = text[0].isLatin()
                                        var current = ""

                                        fun startTag() {
                                            builder.startTag()
                                            if (!currentlyLatin) {
                                                builder.font("rodin").bold(false)
                                            }
                                        }
                                        fun endTag() = builder.text(current).endTag()

                                        startTag()
                                        for (c in text) {
                                            val cLatin = c.isLatin()
                                            if (cLatin != currentlyLatin) {
                                                endTag()
                                                currentlyLatin = cLatin
                                                current = "$c"
                                                startTag()
                                            } else {
                                                current += c
                                            }
                                        }
                                        endTag()
                                    }
                                    
                                    val primarySourceMaterial = songInfo.songNameAndSource.songSourceMaterial
                                    this += TextLabel(songInfo.songNameAndSource.songNameWithLineBreaks, font = main.fontRobotoBold).apply {
                                        this.markup.set(markupNormal)
                                        this.bounds.height.bind {
                                            6f * 4 * (if ('\n' in text.use()) 2 else 1)
                                        }
                                        this.textColor.set(Color.WHITE)
                                        this.renderAlign.set(RenderAlign.center)
                                        this.internalTextBlock.bind {
                                            val builder = markup.use()!!.Builder()
                                            
                                            // Song name
                                            builder.startTag().bold()
                                            parseNonlatin(builder, text.use())
                                            builder.endTag()
                                            
                                            builder.build()
                                        }
                                    }
                                    if (primarySourceMaterial != null) {
                                        this += TextLabel("", font = main.fontRobotoBold).apply {
                                            this.markup.set(markupCondensed)
                                            this.bounds.height.set(4f * 4)
                                            this.textColor.set(Color.WHITE)
                                            this.renderAlign.set(RenderAlign.center)
                                            this.internalTextBlock.bind {
                                                val builder = markup.use()!!.Builder()

                                                // Song source (game)
                                                // Make sure to switch to Rodin for non-latin text
                                                val src = primarySourceMaterial
                                                builder.scale(0.75f).startTag()
                                                parseNonlatin(builder, src)
                                                builder.endTag()

                                                builder.build()
                                            }
                                        }
                                    }
                                    this += TextLabel(songInfo.songArtist, font = main.fontRobotoBold).apply {
                                        this.markup.set(markupNormal)
                                        this.bounds.height.set(4f * 4)
                                        this.textColor.set(Color.WHITE)
                                        this.renderAlign.set(RenderAlign.center)
                                        this.internalTextBlock.bind {
                                            val builder = markup.use()!!.Builder()
                                            
                                            // Song artist
                                            builder.scale(0.75f).text(text.use()).endTag()
                                            
                                            builder.build()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
        
        currentInboxItem.addListener {
            val inboxItem = it.getOrCompute()
            globalVbox.removeAllChildren()
            
            if (inboxItem != null) {
                updateForInboxItem(inboxItem)
            }
        }
    }

    private fun createInboxItemUI(item: InboxItem): UIElement {
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
                        this += VBox().apply {
                            val rowHeight = 7f * 4
                            this.bounds.height.set(rowHeight * fields.size)
                            this.temporarilyDisableLayouts {
                                fields.forEachIndexed { i, (key, value) ->
                                    this += Pane().apply {
                                        this.bounds.height.set(rowHeight)
                                        this.margin.set(Insets(0.5f * 4, 0f))
                                        this += TextLabel({
                                            "[b]${StoryL10N.getVar("inboxItem.infoMaterial.${key}").use()}[] ${value.use()}"
                                        }, font = main.fontRobotoBold).apply {
                                            this.markup.set(slabMarkup)
                                            this.textColor.set(Color.BLACK)
                                            this.renderAlign.set(Align.center)
                                            this.padding.set(Insets(2f, 2f, 0f, 10f))
                                        }
                                    }
                                }
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
                item as IContractDoc
                val subtype: ContractSubtype = if (item is InboxItem.ContractDoc) item.subtype else if (item is InboxItem.PlaceholderContract) item.subtype else ContractSubtype.NORMAL
                val paper = createPaperTemplate(when (subtype) {
                    ContractSubtype.NORMAL -> "desk_contract_full"
                    ContractSubtype.TRAINING -> "desk_contract_paper"
                })
                
                val headingText: ReadOnlyVar<String> = when (item) {
                    is InboxItem.ContractDoc -> item.headingText
                    is InboxItem.PlaceholderContract -> item.headingText
                    else -> "<missing heading text>".asReadOnlyVar()
                }
                
                paper.paperPane += VBox().apply {
                    this.spacing.set(1f * 4)
                    this.temporarilyDisableLayouts {
                        val useLongCompanyName = item.hasLongCompanyName
                        this += Pane().apply {
                            if (useLongCompanyName) {
                                this.bounds.height.set(13f * 4)
                            } else {
                                this.bounds.height.set(12f * 4)
                            }
                            this.margin.set(Insets(0f, 2.5f * 4, 0f, 0f))

                            this += TextLabel(headingText, font = main.fontMainMenuHeading).apply {
                                this.bindWidthToParent(multiplier = 0.5f, adjust = -2f * 4)
                                this.padding.set(Insets(0f, 0f, 0f, 1f * 4))
                                this.textColor.set(Color.BLACK)
                                if (useLongCompanyName) {
                                    this.renderAlign.set(Align.topLeft)
                                    this.setScaleXY(0.6f)
                                } else {
                                    this.renderAlign.set(Align.left)
                                }
                            }
                            this += Pane().apply {
                                Anchor.TopRight.configure(this)
                                this.bindWidthToParent(multiplier = 0.5f, adjust = -2f * 4)

                                this += TextLabel(item.name, font = main.fontRobotoMonoBold).apply {
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(Align.topRight)
                                }
                                if (!useLongCompanyName) { // Right-aligned company name
                                    this += TextLabel(item.requester.localizedName, font = main.fontRobotoCondensedItalic).apply {
                                        this.textColor.set(Color.BLACK)
                                        this.renderAlign.set(Align.bottomRight)
                                    }
                                }
                            }
                            
                            if (useLongCompanyName) {
                                // Centred long company name
                                this += TextLabel(item.requester.localizedName, font = main.fontRobotoCondensedItalic).apply {
                                    this.textColor.set(Color.BLACK)
                                    this.renderAlign.set(Align.bottom)
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

    inner class InboxItemListingObj(val inboxItem: InboxItem) : ActionablePane(), Toggle {
        override val selectedState: BooleanVar = BooleanVar(false)
        override val toggleGroup: Var<ToggleGroup?> = Var(null)
        
        private val currentInboxItemState: ReadOnlyVar<InboxItemState> = scenario.inboxState.itemStateVarOrUnavailable(inboxItem.id)
        private val useFlowFont: ReadOnlyBooleanVar = BooleanVar { currentInboxItemState.use() == InboxItemState.Unavailable }

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
                    if (state is InboxItemState.Available && state.newIndicator) {
                        reg!!.setRegion(availableBlinkTexRegs[blinkFrameIndex.use()])
                    } else {
                        reg!!.setRegion(StoryAssets.get<Texture>("desk_inboxitem_${
                            when (state) {
                                is InboxItemState.Unavailable -> "unavailable"
                                is InboxItemState.Available -> "available"
                                is InboxItemState.Completed -> "cleared"
                                is InboxItemState.Skipped -> "skipped"
                            }
                        }"))
                    }
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

            titleAreaPane += TextLabel("", font = inboxItemTitleFont).apply {
                this.text.bind {
                    val listingName = inboxItem.listingName.use()
                    if (useFlowFont.use()) {
                        listingName.replace('-', ' ')
                    } else listingName
                }
                this.font.bind { 
                    if (useFlowFont.use()) main.fontFlowCircular else inboxItemTitleFont
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