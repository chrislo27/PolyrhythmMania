package polyrhythmmania.storymode.screen.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.Paintbox
import paintbox.binding.*
import paintbox.font.Markup
import paintbox.font.PaintboxFont
import paintbox.ui.*
import paintbox.ui.animation.Animation
import paintbox.ui.area.Insets
import paintbox.ui.control.*
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.grey
import paintbox.util.gdxutils.isAltDown
import paintbox.util.gdxutils.isControlDown
import paintbox.util.gdxutils.isShiftDown
import paintbox.util.wave.WaveUtils
import polyrhythmmania.PRMania
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.StoryAssets
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.StorySession
import polyrhythmmania.storymode.contract.Contracts
import polyrhythmmania.storymode.inbox.*
import polyrhythmmania.ui.PRManiaSkins
import polyrhythmmania.ui.TogglableInputProcessor
import java.time.LocalDateTime
import kotlin.math.absoluteValue

class DesktopUI(
        val scenario: DesktopScenario,
        private val controllerFactory: (DesktopUI) -> DesktopController,
        val rootScreen: AbstractDesktopScreen,
) : Disposable {
    
    companion object {
        const val UI_SCALE: Int = 4
        private const val ITEMS_VISIBLE_AT_ONCE: Int = 7
    }

    val main: PRManiaGame = rootScreen.main
    val storySession: StorySession get() = rootScreen.storySession
    var debugFeaturesEnabled: Boolean = false
    
    val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 320f * UI_SCALE, 180f * UI_SCALE) // 320x180 is the virtual resolution. Needs to be 4x (1280x720) for font scaling
        this.update()
    }
    val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    val sceneRoot: SceneRoot = SceneRoot(uiViewport).apply { 
        this.doClipping.set(true)
    }
    private val keystrokeInputProcessor: KeystrokeInputProcessor = this.KeystrokeInputProcessor()
    private val inputProcessor: TogglableInputProcessor = TogglableInputProcessor(InputMultiplexer(sceneRoot.inputSystem, keystrokeInputProcessor))
    
    val controller: DesktopController by lazy { controllerFactory(this) }
    val animations: DesktopAnimations = DesktopAnimations(this, inputProcessor)
    val dialogHandler: DesktopDialogHandler by lazy { DesktopDialogHandler(this) }
    val background: DesktopBackground by lazy { DesktopBackground(this.uiCamera) }
    val bonusMusic: BonusMusic by lazy { BonusMusic(this) }
    
    private val availableBlinkTexRegs: List<TextureRegion> = run {
        val numFrames = 5
        (0 until numFrames).map { i -> TextureRegion(StoryAssets.get<Texture>("desk_inboxitem_available_blink_$i")) }
    }
    private val blinkFrameIndex: ReadOnlyIntVar = IntVar {
        val secPerFrame = 0.15f
        val frames = availableBlinkTexRegs.size
        (WaveUtils.getTriangleWave(secPerFrame * frames * 2) * frames).toInt().coerceIn(0, frames - 1)
    }
    
    val inboxItemRenderer: InboxItemRenderer = InboxItemRenderer(this)
    private val inboxItemTitleFont: PaintboxFont = main.fontLexendBold
    private val inboxItemSubtitleFont: PaintboxFont = main.fontLexend
    val monoMarkup: Markup get() = inboxItemRenderer.monoMarkup
    val slabMarkup: Markup get() = inboxItemRenderer.slabMarkup
    val robotoRegularMarkup: Markup get() = inboxItemRenderer.robotoRegularMarkup
    val robotoBoldMarkup: Markup get() = inboxItemRenderer.robotoBoldMarkup
    val robotoCondensedMarkup: Markup get() = inboxItemRenderer.robotoCondensedMarkup
    val openSansMarkup: Markup get() = inboxItemRenderer.openSansMarkup
    
    val currentInboxItem: ReadOnlyVar<InboxItem?>
    val currentInboxItemState: ReadOnlyVar<InboxItemState>
    val bgElement: UIElement
    val rightSideInfoPane: DesktopInfoPane
    
    val inboxItemListScrollbar: ScrollBar
    private val inboxItemListingObjs: List<InboxItemListingObj>
    private val frameScrollPane: ScrollPane
    private val itemsVbox: VBox
    
    init { // Background, base settings
        sceneRoot.debugOutlineColor.set(Color(1f, 0f, 0f, 1f))

        sceneRoot += NoInputPane().apply {
            this += object : Pane() { // renderUpdate hook
                override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
                    renderUpdate()
                }
            }.apply { 
                this.bounds.width.set(0f)
                this.bounds.height.set(0f)
            }
        }
        bgElement = Pane().apply {
            this += Button(StoryL10N.getVar("desktop.menu"), font = main.fontRobotoBold).apply {
                this.bounds.width.set(24f * UI_SCALE)
                this.bounds.height.set(11f * UI_SCALE)
                this.skinID.set(PRManiaSkins.BUTTON_SKIN_STORY_DARK)
                Anchor.TopLeft.configure(this, offsetX = 2f * UI_SCALE, offsetY = 2f * UI_SCALE)
                this.setOnAction {
                    openMenuDialog()
                }
            }
        }
        sceneRoot += bgElement
    }

    init { // Left scroll area and inbox item view
        val frameImg = ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_inboxitem_frame"))).apply {
            this.bounds.x.set(16f * UI_SCALE)
            this.bounds.y.set(14f * UI_SCALE)
            this.bounds.width.set(86f * UI_SCALE)
            this.bounds.height.set(152f * UI_SCALE)
            this.doClipping.set(true) // Safety clipping so nothing exceeds the overall frame
        }
        bgElement += frameImg
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

        frameScrollPane = ScrollPane().apply {
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
        bgElement += inboxItemListScrollbar
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

        itemsVbox = VBox().apply {
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
        inboxItemListingObjs = scenario.inboxItems.items.map { ii ->
            InboxItemListingObj(ii)
        }
        fun addObj(obj: InboxItemListingObj) {
            val inboxItem = obj.inboxItem
            
            val heading = inboxItem.heading
            if (heading != null) {
                itemsVbox += InboxItemHeadingObj(heading, inboxItem)
            }

            itemsVbox += obj
            itemToggleGroup.addToggle(obj)
        }
        inboxItemListingObjs.forEach { addObj(it) }


        val inboxItemDisplayPane: UIElement = VBox().apply {
            this.bounds.x.set(104f * UI_SCALE)
            this.bounds.width.set(128f * UI_SCALE)
            this.align.set(VBox.Align.CENTRE)
        }
        bgElement += inboxItemDisplayPane

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
            this.spacing.set(3f * UI_SCALE)
            
            inputProcessor.enabled.addListener {
                val animations = this@DesktopUI.sceneRoot.animations
                if (it.getOrCompute()) {
                    if (!visible.get()) {
                        visible.set(true)
                        opacity.set(0f)
                        animations.enqueueAnimation(Animation(Interpolation.linear, 0.4f, 0f, 1f), opacity)
                    }
                } else {
                    if (visible.get()) {
                        animations.cancelAnimationFor(opacity)
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
        bgElement += rightSideInfoPane
        
        currentInboxItem.addListener {
            val inboxItem = it.getOrCompute()
            rightSideInfoPane.removeAllChildren()
            
            if (inboxItem != null) {
                rightSideInfoPane.updateForInboxItem(inboxItem)
            }
        }
    }
    
    fun render(batch: SpriteBatch) {
        batch.projectionMatrix = this.uiCamera.combined
        batch.begin()
        uiViewport.apply()
        background.render(batch, scenario.inboxState.anyAvailable.get())
        this.sceneRoot.renderAsRoot(batch)
        batch.end()
    }

    fun getTargetVbarValueForInboxItem(inboxItem: InboxItem): Float {
        val currentValue = inboxItemListScrollbar.value.get()
        val vbox = itemsVbox
        val listingObj: InboxItemListingObj = vbox.children.find {
            it is InboxItemListingObj && it.inboxItem == inboxItem
        } as? InboxItemListingObj ?: return currentValue
        
        return getTargetVbarValueForListingItem(listingObj)
    }
    
    fun getTargetVbarValueForListingItem(listingObj: UIElement): Float {
        val scrollBar = inboxItemListScrollbar
        val currentValue = scrollBar.value.get()
        
        val listingTop = listingObj.bounds.y.get()
        val listingSize = listingObj.bounds.height.get()
        val listingBottom = listingTop + listingSize

        val scrollPane = frameScrollPane
        val contentPane = scrollPane.contentPane
        val windowTop = contentPane.contentOffsetY.get().absoluteValue
        val windowSize = scrollPane.contentPaneHeight.get()
        val windowBottom = windowTop + windowSize
        
        // Check if listing is fully within view: top >= windowTop AND bottom <= windowBottom
        if (listingTop >= windowTop && listingBottom <= windowBottom) {
            // No changes needed
            return currentValue
        }
        
        // Note: If the listing item is bigger than the window size, then default to scrolling to the top of that item
        val newWindowTop: Float = if ((listingTop < windowTop) || (listingSize > windowSize)) {
            // Scroll to top edge
            listingTop
        } else {
            // Scroll to bottom edge
            listingBottom - windowSize
        }
        
        // Map newWindowTop to a scrollbar position
        val contentHeightDiff = scrollPane.contentHeightDiff.get()
        val newWindowTopPercentage = (newWindowTop / contentHeightDiff).coerceIn(0f, 1f)

        return MathUtils.lerp(scrollBar.minimum.get(), scrollBar.maximum.get(), newWindowTopPercentage)
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
                    background.sendEnvelope()
                })
                animations.enqueueAnimation(animations.AnimScrollBar(getTargetVbarValueForInboxItem(item)))
            }
            
            animations.enqueueAnimation(DesktopAnimations.AnimDelay(0.5f))
            if (futureItems.isNotEmpty()) {
                animations.enqueueAnimation(animations.AnimScrollBar(getTargetVbarValueForInboxItem(futureItems.first())))
            }
            animations.enqueueAnimation(animations.AnimLockInputs(false))
        }

        storySession.musicHandler.transitionToDesktopMix(if (this.debugFeaturesEnabled) scenario.inboxState else null)
    }
    
    private fun renderUpdate() {
        blinkFrameIndex.invalidate()
        animations.frameUpdate(Gdx.graphics.deltaTime)
    }

    private fun openMenuDialog() {
        controller.playSFX(DesktopController.SFXType.PAUSE_ENTER)
        dialogHandler.openDialog(DesktopDialogMenu(this@DesktopUI))
        bonusMusic.isPlaying.set(false)
        storySession.musicHandler.transitionToBandpass(true)
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
    
    
    inner class KeystrokeInputProcessor : InputAdapter() {
        override fun keyDown(keycode: Int): Boolean {
            val currentInboxItem = currentInboxItem.getOrCompute()
            when (keycode) {
                Keys.ESCAPE -> {
                    onEscapePressed(currentInboxItem)
                    return true
                }
                Keys.UP, Keys.DOWN -> {
                    val dir = if (keycode == Keys.UP) -1 else 1
                    if (currentInboxItem != null) {
                        selectNextInboxItem(currentInboxItem, dir)
                    }
                }
            }
            
            return false
        }
        
        private fun selectNextInboxItem(currentInboxItem: InboxItem, dir: Int) {
            val indexOfCurrent = inboxItemListingObjs.indexOfFirst { it.inboxItem == currentInboxItem }
            if (indexOfCurrent != -1) {
                var i = indexOfCurrent + dir

                while (i in 0 until inboxItemListingObjs.size) {
                    val obj = inboxItemListingObjs[i]
                    if (obj.myInboxItemState.getOrCompute().completion != InboxItemCompletion.UNAVAILABLE) {
                        val targetPos = getTargetVbarValueForInboxItem(obj.inboxItem)
                        if (inboxItemListScrollbar.value.get() != targetPos) {
                            animations.cancelAnimations { it is DesktopAnimations.AnimScrollBar }
                            animations.enqueueAnimation(animations.AnimScrollBar(targetPos))
                        }
                        
                        obj.action(true)
                        break
                    }
                    i += dir
                }
            }
        }
        
        private fun onEscapePressed(currentInboxItem: InboxItem?) {
            if (dialogHandler.isDialogOpen()) {
                val currentDialog = dialogHandler.getActiveDialog() ?: return
                if (currentDialog !is DesktopDialog) {
                    dialogHandler.closeDialog()
                } else if (currentDialog.canCloseWithEscKey()) {
                    currentDialog.attemptClose()
                }
            } else if (currentInboxItem != null) {
                inboxItemListingObjs.find { it.inboxItem == currentInboxItem }?.action(false)
            } else {
                openMenuDialog()
            }
        }
    }

    inner class InboxItemListingObj(val inboxItem: InboxItem) : ActionablePane(), Toggle {
        override val selectedState: BooleanVar = BooleanVar(false)
        override val toggleGroup: Var<ToggleGroup?> = Var(null)
        
        val myInboxItemState: ReadOnlyVar<InboxItemState> = scenario.inboxState.itemStateVarOrUnavailable(inboxItem.id)
        private val useFlowFont: ReadOnlyBooleanVar = BooleanVar { myInboxItemState.use().completion == InboxItemCompletion.UNAVAILABLE }
        
        private val contractListingName: ReadOnlyVar<String?> = Var.bind {
            if (inboxItem is InboxItem.ContractDoc 
                    && (myInboxItemState.use().playedBefore || (Paintbox.debugMode.use() && PRMania.enableEarlyAccessMessage))) {
                inboxItem.contractListingName?.use() 
            } else null
        }

        init {
            this.bounds.width.set(78f * UI_SCALE)
            this.bounds.height.set(20f * UI_SCALE)

            val contentPane = NoInputPane().apply {
                this.margin.set(Insets(1f * UI_SCALE, 0f * UI_SCALE, 1f * UI_SCALE, 1f * UI_SCALE))
            }
            this += contentPane

            // Base texture
            if (inboxItem is InboxItem.EmploymentContract) {
                contentPane += ImageNode().apply { 
                    this.textureRegion.sideEffecting(TextureRegion()) { reg ->
                        val state = myInboxItemState.use()
                        if (state.completion == InboxItemCompletion.UNAVAILABLE) {
                            reg!!.setRegion(StoryAssets.get<Texture>("desk_inboxitem_unavailable"))
                        } else {
                            reg!!.setRegion(StoryAssets.get<Texture>("desk_inboxitem_employment_${
                                when (state.completion) {
                                    InboxItemCompletion.AVAILABLE -> "unsigned_blue"
                                    InboxItemCompletion.COMPLETED -> "signed_green"
                                    InboxItemCompletion.SKIPPED -> "signed_red"
                                    else -> throw IllegalStateException("Unhandled impossible employment contract inbox item state completion: ${state.completion}")
                                }
                            }"))
                        }

                        reg
                    }
                }
            } else {
                if (inboxItem is InboxItem.ContractDoc && inboxItem.isSuperHard) {
                    contentPane += ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_inboxitem_blank_red")))
                } else {
                    contentPane += ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_inboxitem_blank")))
                }
            }
            // LED indicator
            contentPane += ImageNode().apply { 
                this.textureRegion.sideEffecting(TextureRegion()) {reg ->
                    val state = myInboxItemState.use()
                    if (inboxItem.id == InboxItem.ContractDoc.getDefaultContractDocID(Contracts.ID_BOSS)) {
                        reg!!.setRegion(StoryAssets.get<Texture>("desk_inboxitem_${
                            when (state.completion) {
                                InboxItemCompletion.AVAILABLE -> "boss_available"
                                InboxItemCompletion.COMPLETED -> "boss_cleared"
                                InboxItemCompletion.UNAVAILABLE -> "unavailable"
                                InboxItemCompletion.SKIPPED -> "skipped"
                            }
                        }"))
                    } else if (state.completion == InboxItemCompletion.AVAILABLE && state.newIndicator) {
                        reg!!.setRegion(availableBlinkTexRegs[blinkFrameIndex.use()])
                    } else {
                        reg!!.setRegion(StoryAssets.get<Texture>("desk_inboxitem_${
                            when (state.completion) {
                                InboxItemCompletion.AVAILABLE -> "available"
                                InboxItemCompletion.COMPLETED -> "cleared"
                                InboxItemCompletion.UNAVAILABLE -> "unavailable"
                                InboxItemCompletion.SKIPPED -> "skipped"
                            }
                        }"))
                    }
                    reg
                }
                this.visible.bind {
                    inboxItem !is InboxItem.EmploymentContract
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
            }
            contentPane += bottomAreaPane

            if (inboxItem !is InboxItem.EmploymentContract) {
                titleAreaPane += TextLabel("", font = inboxItemTitleFont).apply {
                    this.text.bind {
                        val listingName = if (inboxItem is InboxItem.ContractDoc && contractListingName.use() == null) {
                            // Show contract code if music info is not available
                            inboxItem.name.use()
                        } else inboxItem.listingName.use()

                        if (useFlowFont.use()) {
                            listingName.replace('-', ' ')
                        } else listingName
                    }
                    this.font.bind {
                        if (useFlowFont.use()) main.fontFlowCircular else inboxItemTitleFont
                    }
                }
            }

            bottomAreaPane += TextLabel("", font = inboxItemSubtitleFont).apply {
                this.text.bind {
                    when (inboxItem) {
                        is InboxItem.ContractDoc -> {
                            if (contractListingName.use() != null) inboxItem.name.use() else ""
                        }

                        is InboxItem.Memo -> {
                            if (!myInboxItemState.use().newIndicator) {
                                StoryL10N.getVar("inboxItem.memo.listingSubtitle", Var {
                                    listOf(inboxItem.shortFrom.use())
                                }).use()
                            } else ""
                        }

                        is InboxItem.RobotTest -> {
                            if (!myInboxItemState.use().newIndicator) {
                                StoryL10N.getVar("inboxItem.robotTest.listingSubtitle", Var {
                                    listOf(inboxItem.listingSubtitle.use())
                                }).use()
                            } else ""
                        }
                        
                        is InboxItem.InfoMaterial -> StoryL10N.getVar("inboxItem.infoMaterial.heading").use()

                        else -> ""
                    }
                }
                this.visible.bind { myInboxItemState.use().completion != InboxItemCompletion.UNAVAILABLE }
                this.renderAlign.set(RenderAlign.left)
                this.textColor.set(Color.DARK_GRAY.cpy())
                this.margin.set(Insets(0f, 0f, 1f * UI_SCALE, 1f * UI_SCALE))
                this.setScaleXY(0.8f)
            }

            // Selector outline/ring
            this += ImageNode(TextureRegion(StoryAssets.get<Texture>("desk_inboxitem_selected_outline"))).apply {
                this.bounds.x.set(-3f * UI_SCALE)
                this.bounds.y.set(-1f * UI_SCALE)
                this.bounds.width.set(84f * UI_SCALE)
                this.bounds.height.set(23f * UI_SCALE)
                this.visible.bind { selectedState.use() }
            }

            this.setOnAction {
                action(null)
            }
            this.setOnAltAction { 
                if (debugFeaturesEnabled && Paintbox.debugMode.get()) {
                    val currentState = myInboxItemState.getOrCompute()

                    scenario.inboxState.putItemState(inboxItem, currentState.copy(completion = InboxItemCompletion.COMPLETED, newIndicator = false, stageCompletionData = StageCompletionData(LocalDateTime.now(), LocalDateTime.now(), 100, Gdx.input.isShiftDown(), true)))
                    updateAndShowNewlyAvailableInboxItems()
                }
            }
        }
        
        fun action(setStateTo: Boolean?) {
            val currentState = myInboxItemState.getOrCompute()
            if (currentState.completion != InboxItemCompletion.UNAVAILABLE) {
                when (setStateTo) {
                    null -> this.selectedState.invert()
                    else -> this.selectedState.set(setStateTo)
                }
                
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

    inner class InboxItemHeadingObj(val heading: Heading, val followingInboxItem: InboxItem) : Pane() {

        val followingInboxItemState: ReadOnlyVar<InboxItemState> = scenario.inboxState.itemStateVarOrUnavailable(followingInboxItem.id)
        private val isFollowingAvailable: ReadOnlyBooleanVar = BooleanVar { followingInboxItemState.use().completion != InboxItemCompletion.UNAVAILABLE }

        init {
            this.bounds.width.set(78f * UI_SCALE)
            this.bounds.height.set(30f * UI_SCALE)

            val contentPane = NoInputPane().apply {
                this.margin.set(Insets(2f * UI_SCALE, 4f * UI_SCALE))
            }
            this += contentPane

            val grey = Color().grey(232 / 255f, a = 1f).toConstVar()
            val image = ImageNode(binding = {
                TextureRegion(heading.getTexture())
            }).apply {
                this.tint.bind {
                    Color(grey.use()).apply {
                        this.a *= 0.8f
                    }
                }
                this.renderAlign.set(RenderAlign.bottomRight)
                this.opacity.set(if (isFollowingAvailable.get()) 1f else 0f) // Not bound, will be animated
            }
            contentPane += image
            contentPane += TextLabel(binding = {
                (if (isFollowingAvailable.use()) heading.text else StoryL10N.getVar("inboxItem.heading.notUnlockedYet")).use()
            }, font = main.fontMainMenuHeading).apply {
                this.bindWidthToParent(multiplier = 0.75f)
                this.textColor.bind { grey.use() }
                this.renderAlign.set(RenderAlign.bottomLeft)
                this.doLineWrapping.set(true)
                this.setScaleXY(0.9f)
                this.lineSpacingMultiplier.set(0.9f)
            }
            
            isFollowingAvailable.addListener {
                if (it.getOrCompute() && image.opacity.get() < 1f) {
                    this@DesktopUI.sceneRoot.animations.enqueueAnimation(Animation(Interpolation.linear, 0.5f, 0f, 1f), image.opacity)
                } else image.opacity.set(0f)
            }
        }
    }
}
