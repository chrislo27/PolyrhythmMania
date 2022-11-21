package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.binding.*
import paintbox.font.Markup
import paintbox.font.PaintboxFont
import paintbox.ui.*
import paintbox.ui.animation.Animation
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.layout.VBox
import paintbox.util.MathHelper
import paintbox.util.gdxutils.isAltDown
import paintbox.util.gdxutils.isControlDown
import paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItemCompletion
import polyrhythmmania.storymode.inbox.InboxItemState
import polyrhythmmania.storymode.test.TestStoryDesktopScreen
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.ui.TogglableInputProcessor

class DesktopUI(
        val scenario: DesktopScenario,
        private val controllerFactory: (DesktopUI) -> DesktopController,
        val rootScreen: TestStoryDesktopScreen, // TODO remove this?
) : Disposable {
    
    companion object {
        const val UI_SCALE: Int = 4
        private const val ITEMS_VISIBLE_AT_ONCE: Int = 7
    }

    val main: PRManiaGame = rootScreen.main
    
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 320f * UI_SCALE, 180f * UI_SCALE) // 320x180 is the virtual resolution. Needs to be 4x (1280x720) for font scaling
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val inputProcessor: TogglableInputProcessor = TogglableInputProcessor(sceneRoot.inputSystem)
    
    val controller: DesktopController by lazy { controllerFactory(this) }
    val animations: DesktopAnimations = DesktopAnimations(this, inputProcessor)
    
    private val availableBlinkTexRegs: List<TextureRegion> = run {
        val numFrames = 5
        (0 until numFrames).map { i -> TextureRegion(StoryAssets.get<Texture>("desk_inboxitem_available_blink_$i")) }
    }
    private val blinkFrameIndex: ReadOnlyIntVar = IntVar {
        val secPerFrame = 0.15f
        val frames = availableBlinkTexRegs.size
        (MathHelper.getTriangleWave(secPerFrame * frames * 2) * frames).toInt().coerceIn(0, frames - 1)
    }
    
    val inboxItemRenderer: InboxItemRenderer = InboxItemRenderer(this)
    private val inboxItemTitleFont: PaintboxFont = main.fontLexendBold
    private val monoMarkup: Markup get() = inboxItemRenderer.monoMarkup
    private val slabMarkup: Markup get() = inboxItemRenderer.slabMarkup
    private val robotoCondensedMarkup: Markup get() = inboxItemRenderer.robotoCondensedMarkup
    private val openSansMarkup: Markup get() = inboxItemRenderer.openSansMarkup
    
    val currentInboxItem: ReadOnlyVar<InboxItem?>
    val currentInboxItemState: ReadOnlyVar<InboxItemState>
    val bg: UIElement
    val rightSideInfoPane: DesktopInfoPane
    
    val inboxItemListScrollbar: ScrollBar
    private val inboxItemListingObjs: List<InboxItemListingObj>
    
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
                    animations.frameUpdate(Gdx.graphics.deltaTime)
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
            this.bounds.x.set(16f * UI_SCALE)
            this.bounds.y.set(14f * UI_SCALE)
            this.bounds.width.set(86f * UI_SCALE)
            this.bounds.height.set(152f * UI_SCALE)
            this.doClipping.set(true) // Safety clipping so nothing exceeds the overall frame
        }
        bg += frameImg
        val frameNoCaps = Pane().apply {// Extra pane is so that the frameChildArea is the one that clips properly internally
            this.margin.set(Insets(7f * UI_SCALE, 5f * UI_SCALE, 1f * UI_SCALE, 1f * UI_SCALE))
        }
        val frameChildArea = Pane().apply {
            this.doClipping.set(true)
            this.padding.set(Insets(0f * UI_SCALE, 0f * UI_SCALE, 3f * UI_SCALE, 3f * UI_SCALE))
        }
        frameNoCaps += frameChildArea
        frameImg += frameNoCaps
        frameImg += ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_inboxitem_frame_cap_top"))).apply {
            Anchor.TopLeft.configure(this, offsetY = 1f * UI_SCALE)
            this.bounds.height.set(7f * UI_SCALE)
        }
        frameImg += ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_inboxitem_frame_cap_bottom"))).apply {
            Anchor.BottomLeft.configure(this, offsetY = -1f * UI_SCALE)
            this.bounds.height.set(7f * UI_SCALE)
        }

        val frameScrollPane = ScrollPane().apply {
            this.contentPane.doClipping.set(false)
            this.hBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            this.vBarPolicy.set(ScrollPane.ScrollBarPolicy.NEVER)
            (this.skin.getOrCompute() as ScrollPaneSkin).bgColor.set(Color(1f, 1f, 1f, 0f))
        }
        frameChildArea += frameScrollPane

        inboxItemListScrollbar = ScrollBar(ScrollBar.Orientation.VERTICAL).apply {
            this.bounds.x.set(2f * UI_SCALE)
            this.bounds.y.set(16f * UI_SCALE)
            this.bounds.width.set(13f * UI_SCALE)
            this.bounds.height.set(148f * UI_SCALE)
            this.skinID.set(PRManiaSkins.SCROLLBAR_SKIN_STORY_DESK)

            this.minimum.set(0f)
            this.maximum.set(100f)
            this.disabled.eagerBind { frameScrollPane.vBar.maximum.use() <= 0f } // Uses the contentHeightDiff internally in ScrollPane
            this.visibleAmount.bind { (17f / 148f) * (maximum.use() - minimum.use()) } // Based on thumb icon height
            this.blockIncrement.eagerBind { (1f / (scenario.inboxItems.items.size - ITEMS_VISIBLE_AT_ONCE).coerceAtLeast(1)) * (maximum.use() - minimum.use()) } // One item at a time

            // Remove up/down arrow buttons
            this.removeChild(this.increaseButton)
            this.removeChild(this.decreaseButton)
            this.thumbArea.bindWidthToParent()
            this.thumbArea.bindHeightToParent()
        }
        frameScrollPane.contentPane.contentOffsetY.eagerBind {
            -inboxItemListScrollbar.value.use() / (inboxItemListScrollbar.maximum.use() - inboxItemListScrollbar.minimum.use()) * frameScrollPane.contentHeightDiff.use()
        }
        bg += inboxItemListScrollbar
        val scrollListener = InputEventListener { event ->
            if (event is Scrolled && !Gdx.input.isControlDown() && !Gdx.input.isAltDown()) {
                val shift = Gdx.input.isShiftDown()
                val vBarAmount = if (shift) event.amountX else event.amountY

                if (vBarAmount != 0f && inboxItemListScrollbar.apparentVisibility.get() && !inboxItemListScrollbar.apparentDisabledState.get()) {
                    if (vBarAmount > 0) inboxItemListScrollbar.incrementBlock() else inboxItemListScrollbar.decrementBlock()
                }
            }
            false
        }
        inboxItemListScrollbar.addInputEventListener(scrollListener)
        frameScrollPane.addInputEventListener(scrollListener)

        val itemsVbox = VBox().apply {
            this.spacing.set(0f)
            this.margin.set(Insets(0f, 1f * UI_SCALE, 0f, 0f))
            this.autoSizeToChildren.set(true)
        }
        frameScrollPane.setContent(itemsVbox)

        val itemToggleGroup = ToggleGroup()
        currentInboxItem = Var.eagerBind {
            val newToggle = itemToggleGroup.activeToggle.use() as? InboxItemListingObj
            newToggle?.inboxItem
        }
        currentInboxItemState = Var.eagerBind {
            val ii = currentInboxItem.use()
            if (ii == null) {
                InboxItemState.DEFAULT_UNAVAILABLE
            } else {
                scenario.inboxState.itemStateVarOrUnavailable(ii.id).use()
            }
        }
        fun addObj(obj: InboxItemListingObj) {
            itemsVbox += obj
            itemToggleGroup.addToggle(obj)
        }
        inboxItemListingObjs = scenario.inboxItems.items.map { ii ->
            // TODO link InboxItem to an UnlockStage; check UnlockStage state
            InboxItemListingObj(ii)
        }
        inboxItemListingObjs.forEach { addObj(it) }


        val inboxItemDisplayPane: UIElement = VBox().apply {
            this.bounds.x.set(104f * UI_SCALE)
            this.bounds.width.set(128f * UI_SCALE)
            this.align.set(VBox.Align.CENTRE)
        }
        bg += inboxItemDisplayPane

        currentInboxItem.addListener {
            val inboxItem = it.getOrCompute()
            inboxItemDisplayPane.removeAllChildren()
            if (inboxItem != null) {
                inboxItemDisplayPane.addChild(inboxItemRenderer.createInboxItemUI(inboxItem))
            }
        }
    }
    
    init {
        rightSideInfoPane = DesktopInfoPane(this).apply {
            this.bounds.x.set(240f * UI_SCALE)
            this.bounds.width.set(72f * UI_SCALE)
            this.margin.set(Insets(15f * UI_SCALE, 0f))
            this.align.set(VBox.Align.BOTTOM)
            this.bottomToTop.set(true)
            this.spacing.set(4f * UI_SCALE)
            
            inputProcessor.enabled.addListener { 
                if (it.getOrCompute()) {
                    if (!visible.get()) {
                        visible.set(true)
                        this@DesktopUI.sceneRoot.animations.enqueueAnimation(Animation(Interpolation.linear, 0.5f, 0f, 1f), opacity)
                    }
                } else {
                    if (visible.get()) {
                        visible.set(false)
//                        this@DesktopUI.sceneRoot.animations.enqueueAnimation(Animation(Interpolation.linear, 0.5f, 1f, 0f).apply {
//                            this.onComplete = {
//                                visible.set(false)
//                            }                                                                                                    
//                        }, opacity)
                    }
                }
            }
        }
        bg += rightSideInfoPane
        
        currentInboxItem.addListener {
            val inboxItem = it.getOrCompute()
            rightSideInfoPane.removeAllChildren()
            
            if (inboxItem != null) {
                rightSideInfoPane.updateForInboxItem(inboxItem)
            }
        }
    }
    
    fun getTargetVbarValueForInboxItem(inboxItem: InboxItem): Float {
        val min = inboxItemListScrollbar.minimum.get()
        val max = inboxItemListScrollbar.maximum.get()
        val currentValue = inboxItemListScrollbar.value.get()
        val totalArea = max - min
        val itemObjs = inboxItemListingObjs
        val objIndex = itemObjs.indexOfFirst { it.inboxItem == inboxItem }.takeUnless { it == -1 } ?: return currentValue

        val currentVisiblePercent = currentValue / totalArea // Represents top edge of scroll area
        val maxScrollItems = (itemObjs.size - ITEMS_VISIBLE_AT_ONCE).toFloat()
        val objIndexUpperPercentage = objIndex / maxScrollItems // If the obj is above the currentVisiblePercent
        val objIndexLowerPercentage = (objIndex - (ITEMS_VISIBLE_AT_ONCE - 1)) / maxScrollItems // If the obj is below the bottom edge (currentVisiblePercent + ITEMS_VISIBLE_AT_ONCE - 1)
        
        return if (objIndexUpperPercentage < currentVisiblePercent) {
            objIndexUpperPercentage * totalArea + min
        } else if (objIndexLowerPercentage > currentVisiblePercent) {
            objIndexLowerPercentage * totalArea + min
        } else currentValue
    }
    
    fun updateAndShowNewlyAvailableInboxItems(lockInputs: Boolean = false) {
        scenario.updateProgression()
        
        val futureItems = scenario.checkItemsThatWillBecomeAvailable()
        if (futureItems.isNotEmpty() || lockInputs) {
            if (lockInputs) {
                animations.enqueueAnimation(animations.AnimLockInputs(true))
            }
            
            futureItems.forEach { item ->
                animations.enqueueAnimation(DesktopAnimations.AnimDelay(0.5f))
                animations.enqueueAnimation(DesktopAnimations.AnimGeneric(0f) { _, _ -> 
                    scenario.updateInboxItemAvailability(listOf(item))
                    controller.playSFX(DesktopController.SFXType.INBOX_ITEM_UNLOCKED)
                })
                animations.enqueueAnimation(animations.AnimScrollBar(0.25f, getTargetVbarValueForInboxItem(item)))
            }
            
            animations.enqueueAnimation(DesktopAnimations.AnimDelay(0.5f))
            animations.enqueueAnimation(animations.AnimScrollBar(0.25f, getTargetVbarValueForInboxItem(futureItems.first())))
            animations.enqueueAnimation(animations.AnimLockInputs(false))
        }
    }

    fun onResize(width: Int, height: Int) {
        uiViewport.update(width, height)
    }

    fun enableInputs() {
        val processor = this.inputProcessor
        main.inputMultiplexer.removeProcessor(processor)
        main.inputMultiplexer.addProcessor(processor)
    }

    fun disableInputs() {
        val processor = this.inputProcessor
        main.inputMultiplexer.removeProcessor(processor)
    }

    override fun dispose() {
    }
    

    inner class InboxItemListingObj(val inboxItem: InboxItem) : ActionablePane(), Toggle {
        override val selectedState: BooleanVar = BooleanVar(false)
        override val toggleGroup: Var<ToggleGroup?> = Var(null)
        
        private val currentInboxItemState: ReadOnlyVar<InboxItemState> = scenario.inboxState.itemStateVarOrUnavailable(inboxItem.id)
        private val useFlowFont: ReadOnlyBooleanVar = BooleanVar { currentInboxItemState.use().completion == InboxItemCompletion.UNAVAILABLE }

        init {
            this.bounds.width.set(78f * UI_SCALE)
            this.bounds.height.set(20f * UI_SCALE)

            val contentPane = Pane().apply {
                this.margin.set(Insets(1f * UI_SCALE, 0f * UI_SCALE, 1f * UI_SCALE, 1f * UI_SCALE))
            }
            this += contentPane

            contentPane += ImageNode().apply { 
                this.textureRegion.sideEffecting(TextureRegion()) {reg ->
                    val state = currentInboxItemState.use()
                    if (state.completion == InboxItemCompletion.AVAILABLE && state.newIndicator) {
                        reg!!.setRegion(availableBlinkTexRegs[blinkFrameIndex.use()])
                    } else {
                        reg!!.setRegion(StoryAssets.get<Texture>("desk_inboxitem_${
                            when (state.completion) {
                                InboxItemCompletion.UNAVAILABLE -> "unavailable"
                                InboxItemCompletion.AVAILABLE -> "available"
                                InboxItemCompletion.COMPLETED -> "cleared"
                                InboxItemCompletion.SKIPPED -> "skipped"
                            }
                        }"))
                    }
                    reg
                }
            }
            val titleAreaPane = Pane().apply {
                this.bounds.x.set((1f + 2) * UI_SCALE)
                this.bounds.y.set(1f * UI_SCALE)
                this.bounds.width.set(62f * UI_SCALE)
                this.bounds.height.set(11f * UI_SCALE)
                this.padding.set(Insets(1f * UI_SCALE))
            }
            contentPane += titleAreaPane
            val bottomAreaPane = Pane().apply {
                this.bounds.x.set(3f * UI_SCALE)
                this.bounds.y.set(13f * UI_SCALE)
                this.bounds.width.set(62f * UI_SCALE)
                this.bounds.height.set(5f * UI_SCALE)
//                this.padding.set(Insets(0f * UI_SCALE, 0f * UI_SCALE, 1f * UI_SCALE, 1f * UI_SCALE))
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
                this.bounds.x.set(-3f * UI_SCALE)
                this.bounds.y.set(-1f * UI_SCALE)
                this.bounds.width.set(84f * UI_SCALE)
                this.bounds.height.set(23f * UI_SCALE)
                this.visible.bind { selectedState.use() }
            }

            this.setOnAction {
                val currentState = currentInboxItemState.getOrCompute()
                if (currentState.completion != InboxItemCompletion.UNAVAILABLE) {
                    this.selectedState.invert()
                    if (currentState.completion == InboxItemCompletion.AVAILABLE) {
                        if (inboxItem.isCompletedWhenRead()) {
                            scenario.inboxState.putItemState(inboxItem, currentState.copy(completion = InboxItemCompletion.COMPLETED, newIndicator = false))
                            updateAndShowNewlyAvailableInboxItems()
                        } else if (currentState.newIndicator) {
                            scenario.inboxState.putItemState(inboxItem, currentState.copy(newIndicator = false))
                            updateAndShowNewlyAvailableInboxItems()
                        }
                    }
                    
                    controller.playSFX(DesktopController.SFXType.CLICK_INBOX_ITEM)
                }
            }
        }
    }
}