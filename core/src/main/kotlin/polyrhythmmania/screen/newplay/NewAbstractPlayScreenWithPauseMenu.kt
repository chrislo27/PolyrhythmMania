package polyrhythmmania.screen.newplay

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.PaintboxGame
import paintbox.binding.BooleanVar
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyBooleanVar
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.transition.*
import paintbox.ui.Anchor
import paintbox.ui.Pane
import paintbox.ui.SceneRoot
import paintbox.ui.UIElement
import paintbox.ui.animation.Animation
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.VBox
import paintbox.util.gdxutils.fillRect
import paintbox.util.gdxutils.prepareStencilMask
import paintbox.util.gdxutils.useStencilMask
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.engine.input.InputKeymapKeyboard
import polyrhythmmania.screen.play.ArrowNode
import polyrhythmmania.screen.play.PauseBackground
import polyrhythmmania.screen.play.PauseOption
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.statistics.PlayTimeType
import space.earlygrey.shapedrawer.ShapeDrawer


@Deprecated("Delete me later")
abstract class NewAbstractPlayScreenWithPauseMenu(
        main: PRManiaGame,
        val playTimeType: PlayTimeType?
) : PRManiaScreen(main) {

    /**
     * Used to render the pause [sceneRoot] for the first frame to reduce stutter.
     */
    private var firstRender: Boolean = true
    
    val batch: SpriteBatch = main.batch
    
    protected val isPaused: ReadOnlyBooleanVar = BooleanVar(false)
    protected val keyboardKeybinds: InputKeymapKeyboard by lazy { main.settings.inputKeymapKeyboard.getOrCompute() }

    protected val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    protected val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    protected val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    protected val pauseMenuInputProcessor: InputProcessor = sceneRoot.inputSystem
    protected val shapeDrawer: ShapeDrawer = ShapeDrawer(batch, PaintboxGame.paintboxSpritesheet.fill)

    protected val pauseBg: PauseBackground = PauseBackground()
    protected val pauseOptions: Var<List<PauseOption>> = Var(emptyList())
    protected val selectedPauseOption: Var<PauseOption?> = Var(null)

    // TODO remove these
    private val panelAnimationValue: FloatVar = FloatVar(0f)
    private var activePanelAnimation: Animation? = null
    private val topPane: Pane
    private val bottomPane: Pane
    private val titleLabel: TextLabel

    init { // TODO remove these
        var nextLayer: UIElement = sceneRoot
        fun addLayer(element: UIElement) {
            nextLayer += element
            nextLayer = element
        }
        addLayer(RectElement(Color(0f, 0f, 0f, 0f)))

        topPane = Pane().apply {
            Anchor.TopLeft.configure(this, offsetY = {
                val h = bounds.height.use()
                -h + panelAnimationValue.use() * h
            })
            this.bindHeightToParent(multiplier = 0.3333f)
            this.bindWidthToSelfHeight(multiplier = 1f / pauseBg.triangleSlope)
            this.padding.set(Insets(36f, 0f, 64f, 0f))
        }
        nextLayer += topPane
        bottomPane = Pane().apply {
            Anchor.BottomRight.configure(this, offsetY = {
                val h = bounds.height.use()
                h + panelAnimationValue.use() * -h
            })
            this.bindWidthToParent(multiplier = 0.6666f)
            this.bindHeightToSelfWidth(multiplier = pauseBg.triangleSlope)
        }
        nextLayer += bottomPane

        val leftVbox = VBox().apply {
            this.spacing.set(16f)
            this.bounds.height.set(300f)
        }
        topPane += leftVbox

        titleLabel = TextLabel(binding = { Localization.getVar("play.pause.title").use() }, font = main.fontPauseMenuTitle).apply {
            this.textColor.set(Color.WHITE)
            this.bounds.height.set(128f)
            this.renderAlign.set(Align.left)
        }

        leftVbox.temporarilyDisableLayouts {
            leftVbox += titleLabel
        }

        val transparentBlack = Color(0f, 0f, 0f, 0.75f)
        bottomPane += TextLabel(keyboardKeybinds.toKeyboardString(true, true), font = main.fontMainMenuRodin).apply {
            Anchor.BottomRight.configure(this)
            this.textColor.set(Color.WHITE)
            this.bounds.width.set(550f)
            this.bounds.height.set(80f)
            this.bgPadding.set(Insets(12f))
            this.renderAlign.set(Align.bottomRight)
            this.textAlign.set(TextAlign.LEFT)
            this.backgroundColor.set(transparentBlack)
            this.renderBackground.set(true)
        }

        val optionsBorderSize = 12f
        val optionsContentHeight = 144f
        val optionsBg = RectElement(transparentBlack).apply {
            Anchor.BottomRight.configure(this, offsetY = -80f, offsetX = -15f)
            this.bounds.width.set(275f + optionsBorderSize * 2)
            this.bounds.height.set(optionsContentHeight + optionsBorderSize * 2)
            this.border.set(Insets(optionsBorderSize))
            this.borderStyle.set(SolidBorder(transparentBlack).apply {
                this.roundedCorners.set(true)
            })
        }
        bottomPane += optionsBg

        val selectedLabelColor = Color(0f, 1f, 1f, 1f)
        val unselectedLabelColor = Color(1f, 1f, 1f, 1f)
        fun createTextLabelOption(option: PauseOption, index: Int, allOptions: List<PauseOption>): TextLabel {
            return TextLabel(binding = { Localization.getVar(option.localizationKey).use() }, font = main.fontMainMenuMain).apply {
                Anchor.TopLeft.configure(this)
                this.disabled.set(!option.enabled)
                this.textColor.bind {
                    if (apparentDisabledState.use()) {
                        Color.GRAY
                    } else if (selectedPauseOption.use() == option) {
                        selectedLabelColor
                    } else {
                        unselectedLabelColor
                    }
                }
                this.bounds.height.set(optionsContentHeight / allOptions.size)
                this.padding.set(Insets(2f, 2f, 12f, 12f))
                this.renderAlign.set(Align.left)
                this.textAlign.set(TextAlign.LEFT)
                this += ArrowNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["arrow_pointer_finger"])).apply {
                    Anchor.CentreLeft.configure(this, offsetY = 4f)
                    this.bounds.height.set(64f)
                    this.bindWidthToSelfHeight()
                    this.bounds.x.bind { -(bounds.width.use() + optionsBorderSize * 2 + 2f) }
                    this.visible.bind { selectedPauseOption.use() == option }
                }
                this.setOnAction {
                    attemptPauseEntrySelection()
                }
                this.setOnHoverStart {
                    changeSelectionTo(option)
                }
            }
        }

        optionsBg += VBox().apply {
            this.spacing.set(0f)

            pauseOptions.addListener {
                val optionList = it.getOrCompute()
                this.removeAllChildren()
                this.temporarilyDisableLayouts {
                    optionList.forEachIndexed { index, op ->
                        this += createTextLabelOption(op, index, optionList)
                    }
                }
            }
        }

        val optionList = mutableListOf<PauseOption>()
        optionList += PauseOption("play.pause.resume", true) {
            unpauseGame(true)
        }
        optionList += PauseOption("play.pause.startOver", false) {
            playMenuSound("sfx_menu_enter_game")

//            val thisScreen: AbstractPlayScreen = this
//            val resetAction: () -> Unit = {
//                resetAndUnpause()
//            }
//            main.screen = TransitionScreen(main, thisScreen, thisScreen,
//                    WipeTransitionHead(Color.BLACK.cpy(), 0.4f), WipeTransitionTail(Color.BLACK.cpy(), 0.4f)).apply {
//                onEntryEnd = resetAction
//                onStart = {
//                    Gdx.input.isCursorCatched = true
//                }
//            }
        }
        optionList += PauseOption("play.pause.quitToMainMenu", true) {
            quitToMainMenu(true)
        }
        this.pauseOptions.set(optionList)
    }
    
    
    protected abstract fun renderGameplay(delta: Float)
    
    protected abstract fun shouldCatchCursor(): Boolean
    

    override fun render(delta: Float) {
        val batch = this.batch

        // JIT optimization so there is less stutter when opening pause menu for the first time
        if (this.firstRender) {
            this.firstRender = false
            batch.begin()
            sceneRoot.renderAsRoot(batch) 
            batch.end()
        }
        
        
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        uiViewport.apply()
        renderGameplay(delta)

        
        val camera = uiCamera
        batch.projectionMatrix = camera.combined
        batch.begin()

        if (isPaused.get()) {
            val width = camera.viewportWidth
            val height = camera.viewportHeight
            val shapeRenderer = main.shapeRenderer
            shapeRenderer.projectionMatrix = camera.combined
            uiViewport.apply()

            batch.setColor(1f, 1f, 1f, 0.5f)
            batch.fillRect(0f, 0f, width, height)
            batch.setColor(1f, 1f, 1f, 1f)

            val pauseBg = this.pauseBg

            val topLeftX1 = topPane.bounds.x.get()
            val topLeftY1 = height - (topPane.bounds.y.get() + topPane.bounds.height.get())
            val topLeftX2 = topLeftX1
            val topLeftY2 = height - (topPane.bounds.y.get())
            val topLeftY3 = topLeftY2
            val topLeftX3 = topPane.bounds.x.get() + topPane.bounds.width.get()
            val botRightX1 = bottomPane.bounds.x.get()
            val botRightY1 = height - (bottomPane.bounds.y.get() + bottomPane.bounds.height.get())
            val botRightX2 = bottomPane.bounds.x.get() + bottomPane.bounds.width.get()
            val botRightY2 = botRightY1
            val botRightX3 = botRightX2
            val botRightY3 = height - (bottomPane.bounds.y.get())
            val triLineWidth = 12f
            shapeRenderer.prepareStencilMask(batch) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                shapeRenderer.triangle(topLeftX1, topLeftY1, topLeftX2, topLeftY2, topLeftX3, topLeftY3)
                shapeRenderer.triangle(botRightX1, botRightY1, botRightX2, botRightY2, botRightX3, botRightY3)
                shapeRenderer.end()
            }.useStencilMask {
                batch.setColor(1f, 1f, 1f, 1f)
                pauseBg.render(delta, batch, camera)
                batch.setColor(1f, 1f, 1f, 1f)
            }

            // Draw lines to hide aliasing
            val shapeDrawer = this.shapeDrawer
            shapeDrawer.setColor(0f, 0f, 0f, 1f)
            shapeDrawer.line(topLeftX1 - triLineWidth, topLeftY1 - triLineWidth * pauseBg.triangleSlope,
                    topLeftX3 + triLineWidth, topLeftY3 + triLineWidth * pauseBg.triangleSlope, triLineWidth, false)
            shapeDrawer.line(botRightX1 - triLineWidth, botRightY1 - triLineWidth * pauseBg.triangleSlope,
                    botRightX3 + triLineWidth, botRightY3 + triLineWidth * pauseBg.triangleSlope, triLineWidth, false)
            shapeDrawer.setColor(1f, 1f, 1f, 1f)
            batch.setColor(1f, 1f, 1f, 1f)

            batch.flush()
            shapeRenderer.projectionMatrix = main.nativeCamera.combined

            sceneRoot.renderAsRoot(batch)
        }

        batch.end()
        batch.projectionMatrix = main.nativeCamera.combined
        
        super.render(delta)
    }

    override fun renderUpdate() {
        super.renderUpdate()
        
        if (!isPaused.get() && playTimeType != null) {
            GlobalStats.updateModePlayTime(playTimeType)
        }
    }

    /**
     * Pauses the game.
     */
    protected open fun pauseGame(playSound: Boolean) {
        (isPaused as BooleanVar).set(true)
        if (shouldCatchCursor()) {
            Gdx.input.isCursorCatched = false
        }
        main.inputMultiplexer.removeProcessor(pauseMenuInputProcessor)
        main.inputMultiplexer.addProcessor(pauseMenuInputProcessor)
        
        selectedPauseOption.set(pauseOptions.getOrCompute().firstOrNull())
        
        pauseBg.randomizeSeed()
        panelAnimationValue.set(0f)
        val ani = Animation(Interpolation.smoother, 0.25f, 0f, 1f).apply {
            onComplete = {
                activePanelAnimation = null
            }
        }
        val oldAni = this.activePanelAnimation
        if (oldAni != null) {
            sceneRoot.animations.cancelAnimation(oldAni)
        }
        this.activePanelAnimation = ani
        sceneRoot.animations.enqueueAnimation(ani, panelAnimationValue)

//        soundSystem.setPaused(true)
        
        if (playSound) {
            playMenuSound("sfx_pause_enter")
        }
    }

    /**
     * Unpauses the game.
     */
    protected open fun unpauseGame(playSound: Boolean) {
        (isPaused as BooleanVar).set(false)
//        val player = engine.soundInterface.getCurrentMusicPlayer(engine.musicData.beadsMusic)
//        if (player != null) {
//            engine.musicData.setMusicPlayerPositionToCurrentSec()
//            player.pause(false)
//        }
//        soundSystem.setPaused(false)
        if (shouldCatchCursor()) {
            Gdx.input.isCursorCatched = true
        }
        main.inputMultiplexer.removeProcessor(pauseMenuInputProcessor)
        
        panelAnimationValue.set(0f)
        if (playSound) {
            playMenuSound("sfx_pause_exit")
        }
    }


    private fun attemptPauseEntrySelection() {
        val pauseOp = selectedPauseOption.getOrCompute()
        if (pauseOp != null && pauseOp.enabled) {
            pauseOp.action()
        }
    }

    private fun quitToMainMenu(playSound: Boolean) {
        val main = this.main
        val currentScreen = main.screen
        Gdx.app.postRunnable {
            val mainMenu = main.mainMenuScreen.prepareShow(doFlipAnimation = true)
            main.screen = TransitionScreen(main, currentScreen, mainMenu,
                    FadeOut(0.25f, Color(0f, 0f, 0f, 1f)), FadeIn(0.125f, Color(0f, 0f, 0f, 1f))).apply {
                this.onEntryEnd = {
                    // FIXME
//                    if (currentScreen is AbstractPlayScreen) {
//                        currentScreen.dispose()
//                        container.disposeQuietly()
//                    }
                }
            }
        }

        if (playSound) {
            Gdx.app.postRunnable {
                playMenuSound("sfx_pause_exit")
            }
        }
    }

    private fun changeSelectionTo(option: PauseOption): Boolean {
        if (selectedPauseOption.getOrCompute() != option && option.enabled) {
            selectedPauseOption.set(option)
            playMenuSound("sfx_menu_blip")
            return true
        }
        return false
    }

    override fun keyDown(keycode: Int): Boolean {
        var consumed = false
        if (main.screen === this) {
            if (isPaused.get()) {
                when (keycode) {
                    Input.Keys.ESCAPE, keyboardKeybinds.pause -> {
                        unpauseGame(true)
                        consumed = true
                    }
                    keyboardKeybinds.buttonDpadUp, keyboardKeybinds.buttonDpadDown -> {
                        val options = this.pauseOptions.getOrCompute()
                        if (options.isNotEmpty() && !options.all { !it.enabled }) {
                            val maxSelectionSize = options.size
                            val incrementAmt = if (keycode == keyboardKeybinds.buttonDpadUp) -1 else 1
                            val currentSelected = this.selectedPauseOption.getOrCompute()
                            val currentIndex = options.indexOf(currentSelected)
                            var increment = incrementAmt
                            var nextIndex: Int
                            do {
                                nextIndex = (currentIndex + increment + maxSelectionSize) % maxSelectionSize
                                if (changeSelectionTo(options[nextIndex])) {
                                    consumed = true
                                    break
                                }
                                increment += incrementAmt
                            } while (nextIndex != currentIndex)
                        }
                    }
                    keyboardKeybinds.buttonA -> {
                        attemptPauseEntrySelection()
                        consumed = true
                    }
                }
            } else {
                when (keycode) { // FIXME
                    Input.Keys.ESCAPE, keyboardKeybinds.pause -> {
                        pauseGame(true)
                        consumed = true
                    }
//                    keyboardKeybinds.buttonDpadUp, keyboardKeybinds.buttonDpadDown,
//                    keyboardKeybinds.buttonDpadLeft, keyboardKeybinds.buttonDpadRight -> {
//                        engine.postRunnable {
//                            engine.inputter.onDpadButtonPressed(false)
//                        }
//                        consumed = true
//                    }
//                    keyboardKeybinds.buttonA -> {
//                        engine.postRunnable {
//                            engine.inputter.onAButtonPressed(false)
//                        }
//                        consumed = true
//                    }
                }
            }
        }

        return consumed || super.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        var consumed = false
//        if (main.screen === this) { // FIXME
//            if (!isPaused.get())  {
//                when (keycode) {
//                    keyboardKeybinds.buttonDpadUp, keyboardKeybinds.buttonDpadDown,
//                    keyboardKeybinds.buttonDpadLeft, keyboardKeybinds.buttonDpadRight -> {
//                        engine.postRunnable {
//                            engine.inputter.onDpadButtonPressed(true)
//                        }
//                        consumed = true
//                    }
//                    keyboardKeybinds.buttonA -> {
//                        engine.postRunnable {
//                            engine.inputter.onAButtonPressed(true)
//                        }
//                        consumed = true
//                    }
//                }
//            }
//        }

        return consumed || super.keyUp(keycode)
    }

    protected fun playMenuSound(id: String, volume: Float = 1f, pitch: Float = 1f, pan: Float = 0f): Pair<Sound, Long> {
        val sound: Sound = AssetRegistry[id]
        val menuSFXVol = main.settings.menuSfxVolume.getOrCompute() / 100f
        val soundID = sound.play(menuSFXVol * volume, pitch, pan)
        return sound to soundID
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        uiViewport.update(width, height)
    }

    override fun showTransition() {
        super.showTransition()
        resize(Gdx.graphics.width, Gdx.graphics.height)
    }

    override fun show() {
        super.show()
        unpauseGame(false)
        if (shouldCatchCursor()) {
            Gdx.input.isCursorCatched = true
        }
    }

    override fun hide() {
        super.hide()
        if (shouldCatchCursor()) {
            Gdx.input.isCursorCatched = false
        }
        main.inputMultiplexer.removeProcessor(pauseMenuInputProcessor)
    }

    override fun getDebugString(): String? {
        return super.getDebugString()
    }

    abstract override fun dispose()
    
}