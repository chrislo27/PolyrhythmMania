package polyrhythmmania.screen.play

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.PaintboxGame
import paintbox.binding.*
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
import paintbox.util.MathHelper
import paintbox.util.gdxutils.*
import paintbox.util.sumOfFloat
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.achievements.Achievements
import polyrhythmmania.container.Container
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.input.*
import polyrhythmmania.library.score.LevelScoreAttempt
import polyrhythmmania.screen.mainmenu.menu.SubmitDailyChallengeScoreMenu
import polyrhythmmania.screen.results.ResultsScreen
import polyrhythmmania.sidemodes.AbstractEndlessMode
import polyrhythmmania.sidemodes.DunkMode
import polyrhythmmania.sidemodes.SideMode
import polyrhythmmania.sidemodes.endlessmode.DailyChallengeScore
import polyrhythmmania.sidemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.statistics.PlayTimeType
import polyrhythmmania.world.EntityRodPR
import polyrhythmmania.world.render.ForceTilesetPalette
import polyrhythmmania.world.render.WorldRenderer
import polyrhythmmania.world.tileset.TilesetPalette
import space.earlygrey.shapedrawer.ShapeDrawer
import java.time.*
import java.util.*
import kotlin.math.*


abstract class AbstractPlayScreen protected constructor(
        main: PRManiaGame, val sideMode: SideMode?, val playTimeType: PlayTimeType,
        val container: Container, val challenges: Challenges,
        val inputCalibration: InputCalibration,
        val resultsBehaviour: ResultsBehaviour
) : PRManiaScreen(main) {
    
    data class PauseOption(val localizationKey: String, val enabled: Boolean, val action: () -> Unit)

    protected val startTimestamp: Instant = Instant.now()
    
    val timing: TimingProvider get() = container.timing
    val soundSystem: SoundSystem
        get() = container.soundSystem ?: error("${this::javaClass.name} requires a non-null SoundSystem in the Container")
    val engine: Engine get() = container.engine
    val renderer: WorldRenderer get() = container.renderer
    val batch: SpriteBatch = main.batch

    protected val isPaused: BooleanVar = BooleanVar(false)
    protected val keyboardKeybinds: InputKeymapKeyboard by lazy { main.settings.inputKeymapKeyboard.getOrCompute() }
    private val endSignalListener: VarChangedListener<Boolean> = VarChangedListener {
        if (it.getOrCompute()) {
            Gdx.app.postRunnable {
                onEndSignalFired()
            }
        }
    }

    protected val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    protected val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    protected val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    protected val inputProcessor: InputProcessor = sceneRoot.inputSystem
    protected val shapeDrawer: ShapeDrawer = ShapeDrawer(batch, PaintboxGame.paintboxSpritesheet.fill)
    
    protected val pauseBg: PauseBackground = this.PauseBackground()
    
    protected val pauseOptions: Var<List<PauseOption>> = Var(emptyList())
    protected val selectedPauseOption: Var<PauseOption?> = Var(null)
    
    private val panelAnimationValue: FloatVar = FloatVar(0f)
    private var activePanelAnimation: Animation? = null
    
    protected val topPane: Pane
    protected val bottomPane: Pane
    protected val titleLabel: TextLabel

    /**
     * Used to render the pause [sceneRoot] for the first frame to reduce stutter.
     */
    private var firstRender: Boolean = true

    init {
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
        optionList += PauseOption(if (engine.autoInputs) "play.pause.resume.robotMode" else "play.pause.resume", true) {
            unpauseGame(true)
        }
        optionList += PauseOption("play.pause.startOver", !(sideMode is EndlessPolyrhythm && sideMode.dailyChallenge != null)) {
            playMenuSound("sfx_menu_enter_game")

            val thisScreen: AbstractPlayScreen = this
            val resetAction: () -> Unit = {
                resetAndUnpause()
            }
            main.screen = TransitionScreen(main, thisScreen, thisScreen,
                    WipeTransitionHead(Color.BLACK.cpy(), 0.4f), WipeTransitionTail(Color.BLACK.cpy(), 0.4f)).apply {
                onEntryEnd = resetAction
                onStart = {
                    Gdx.input.isCursorCatched = true
                }
            }
        }
        optionList += PauseOption("play.pause.quitToMainMenu", true) {
            quitToMainMenu(true)
        }
        this.pauseOptions.set(optionList)
    }

    init {
        engine.endSignalReceived.addListener(endSignalListener)
    }

    abstract fun copyScreenForResults(scoreObj: Score, resultsBehaviour: ResultsBehaviour): AbstractPlayScreen

    /**
     * Will be triggered in the gdx main thread.
     */
    open fun onEndSignalFired() {
        soundSystem.setPaused(true)
        container.world.entities.filterIsInstance<EntityRodPR>().forEach { rod ->
            engine.inputter.submitInputsFromRod(rod)
        }
        if (resultsBehaviour is ResultsBehaviour.ShowResults) {
            transitionToResults(resultsBehaviour)
        } else {
            val sideMode = this.sideMode
            if (sideMode is EndlessPolyrhythm && sideMode.dailyChallenge != null) {
                val menuCol = main.mainMenuScreen.menuCollection
                val score: DailyChallengeScore = main.settings.endlessDailyChallenge.getOrCompute()
                val nonce = sideMode.dailyChallengeUUIDNonce.getOrCompute()
                if (score.score > 0 && !engine.autoInputs) {
                    val submitMenu = SubmitDailyChallengeScoreMenu(menuCol, sideMode.dailyChallenge, nonce, score)
                    menuCol.addMenu(submitMenu)
                    menuCol.pushNextMenu(submitMenu, instant = true, playSound = false)
                }

                quitToMainMenu(false)
            } else {
                if (sideMode is DunkMode) {
                    val localDateTime = LocalDateTime.ofInstant(startTimestamp, ZoneId.systemDefault())
                    if (localDateTime.dayOfWeek == DayOfWeek.FRIDAY && localDateTime.toLocalTime() >= LocalTime.of(17, 0)) {
                        Achievements.awardAchievement(Achievements.dunkFridayNight)
                    }
                }
                quitToMainMenu(false)
            }
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val batch = this.batch
        
        if (this.firstRender) {
            this.firstRender = false
            batch.begin()
            sceneRoot.renderAsRoot(batch) // Optimization so there is less stutter when opening pause menu for the first time
            batch.end()
        }
        
        uiViewport.apply()
        renderer.render(batch)

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

        if (!isPaused.get() && timing is SimpleTimingProvider) {
            timing.seconds += Gdx.graphics.deltaTime
            GlobalStats.updateModePlayTime(playTimeType)
        }
    }

    private fun transitionToResults(resultsBehaviour: ResultsBehaviour.ShowResults) {
        val inputter = engine.inputter
        val inputsHit = inputter.inputResults.count { it.inputScore != InputScore.MISS }
        val nInputs = max(inputter.totalExpectedInputs, inputter.minimumInputCount)
        val rawScore: Float = (if (nInputs <= 0) 0f else ((inputter.inputResults.map { it.inputScore }.sumOfFloat { inputScore ->
            inputScore.weight
        } / nInputs) * 100))
        val score: Int = rawScore.roundToInt().coerceIn(0, 100)
        
        val resultsText = container.resultsText
        val ranking = Ranking.getRanking(score)
        val leftResults = inputter.inputResults.filter { it.inputType == InputType.DPAD }
        val rightResults = inputter.inputResults.filter { it.inputType == InputType.A }
        val badLeftGoodRight = leftResults.isNotEmpty() && rightResults.isNotEmpty()
                && (leftResults.sumOfFloat { abs(it.accuracyPercent) } / leftResults.size) - 0.15f > (rightResults.sumOfFloat { abs(it.accuracyPercent) } / rightResults.size)
        val lines: Pair<String, String> = resultsText.generateLinesOfText(score, badLeftGoodRight)
        var isNewHighScore = false
        if (sideMode != null && sideMode is AbstractEndlessMode) {
            val endlessModeScore = sideMode.prevHighScore
            val prevScore = endlessModeScore.highScore.getOrCompute()
            if (score > prevScore) {
                endlessModeScore.highScore.set(score)
                PRManiaGame.instance.settings.persist()
                isNewHighScore = true
            }
        } else if (resultsBehaviour.previousHighScore != null) {
            if (score > resultsBehaviour.previousHighScore && resultsBehaviour.previousHighScore >= 0) {
                isNewHighScore = true
            }
        }
        
        val scoreObj = Score(score, rawScore, inputsHit, nInputs,
                inputter.skillStarGotten.get() && inputter.skillStarBeat.isFinite(), inputter.noMiss,
                challenges,
                resultsText.title ?: Localization.getValue("play.results.defaultTitle"),
                lines.first, lines.second,
                ranking, isNewHighScore
        )

        transitionAway(ResultsScreen(main, scoreObj, container, sideMode, {
            copyScreenForResults(scoreObj, resultsBehaviour)
        }, keyboardKeybinds,
                LevelScoreAttempt(System.currentTimeMillis(), scoreObj.scoreInt, scoreObj.noMiss, scoreObj.skillStar, scoreObj.challenges),
                resultsBehaviour.onRankingRevealed), disposeContainer = false) {}
    }

    private inline fun transitionAway(nextScreen: Screen, disposeContainer: Boolean, action: () -> Unit) {
        main.inputMultiplexer.removeProcessor(inputProcessor)
        Gdx.input.isCursorCatched = false

        action.invoke()

        main.screen = TransitionScreen(main, this, nextScreen,
                FadeOut(0.5f, Color(0f, 0f, 0f, 1f)), FadeIn(0.125f, Color(0f, 0f, 0f, 1f))).apply {
            this.onEntryEnd = {
                this@AbstractPlayScreen.dispose()
                if (disposeContainer) {
                    container.disposeQuietly()
                }
            }
        }
    }
    
    protected fun applyForceTilesetPaletteSettings() {
        when (container.globalSettings.forceTilesetPalette) {
            ForceTilesetPalette.NO_FORCE ->
                container.world.tilesetPalette.applyTo(container.renderer.tileset)
            ForceTilesetPalette.FORCE_PR1 ->
                TilesetPalette.createGBA1TilesetPalette().applyTo(container.renderer.tileset)
            ForceTilesetPalette.FORCE_PR2 ->
                TilesetPalette.createGBA2TilesetPalette().applyTo(container.renderer.tileset)
            ForceTilesetPalette.ORANGE_BLUE ->
                TilesetPalette.createOrangeBlueTilesetPalette().applyTo(container.renderer.tileset)
        }
    }

    /**
     * Resets the entire game state but does not change the pause state
     */
    protected fun resetGameState() {
        // Reset/clearing pass
        engine.removeEvents(engine.events.toList())
        engine.inputter.areInputsLocked = engine.autoInputs
        engine.inputter.reset()
        engine.soundInterface.clearAllNonMusicAudio()
        engine.inputCalibration = this.inputCalibration
        engine.removeActiveTextbox(unpauseSoundInterface = false, runTextboxOnComplete = false)
        engine.resetEndSignal()
        renderer.resetAnimations()
        container.world.resetWorld()
        challenges.applyToEngine(engine)
        
        // Set everything else
        applyForceTilesetPaletteSettings()
        container.setTexturePackFromSource()
        
        timing.seconds = -(1f + max(0f, this.inputCalibration.audioOffsetMs / 1000f))
        engine.seconds = timing.seconds
        val player = engine.soundInterface.getCurrentMusicPlayer(engine.musicData.beadsMusic)
        if (player != null) { // Set music player position
            val musicSample = player.musicSample
            musicSample.moveStartBuffer(0)
            engine.musicData.setMusicPlayerPositionToCurrentSec()
            player.pause(false)
        }
        soundSystem.startRealtime() // Does nothing if already started
        
        val blocks = container.blocks.toList()
        engine.addEvents(blocks.flatMap { it.compileIntoEvents() })
    }

    /**
     * Should be called when first loading this screen so the sound system starts up.
     */
    fun resetAndUnpause() {
        resetGameState()
        unpauseGame(false)
    }

    /**
     * Pauses the game.
     */
    protected open fun pauseGame(playSound: Boolean) {
        isPaused.set(true)
        soundSystem.setPaused(true)
        Gdx.input.isCursorCatched = false
        main.inputMultiplexer.removeProcessor(inputProcessor)
        main.inputMultiplexer.addProcessor(inputProcessor)
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
        
        if (playSound) {
            playMenuSound("sfx_pause_enter")
        }
    }

    /**
     * Unpauses the game.
     */
    protected open fun unpauseGame(playSound: Boolean) {
        isPaused.set(false)
        val player = engine.soundInterface.getCurrentMusicPlayer(engine.musicData.beadsMusic)
        if (player != null) {
            engine.musicData.setMusicPlayerPositionToCurrentSec()
            player.pause(false)
        }
        soundSystem.setPaused(false)
        Gdx.input.isCursorCatched = true
        main.inputMultiplexer.removeProcessor(inputProcessor)
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
                    if (currentScreen is PlayScreen) {
                        currentScreen.dispose()
                        container.disposeQuietly()
                    }
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
                when (keycode) {
                    Input.Keys.ESCAPE, keyboardKeybinds.pause -> {
                        pauseGame(true)
                        consumed = true
                    }
                    keyboardKeybinds.buttonDpadUp, keyboardKeybinds.buttonDpadDown,
                    keyboardKeybinds.buttonDpadLeft, keyboardKeybinds.buttonDpadRight -> {
                        engine.postRunnable {
                            engine.inputter.onDpadButtonPressed(false)
                        }
                        consumed = true
                    }
                    keyboardKeybinds.buttonA -> {
                        engine.postRunnable {
                            engine.inputter.onAButtonPressed(false)
                        }
                        consumed = true
                    }
                }
            }
        }

        return consumed || super.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        var consumed = false
        if (main.screen === this) {
            if (!isPaused.get())  {
                when (keycode) {
                    keyboardKeybinds.buttonDpadUp, keyboardKeybinds.buttonDpadDown,
                    keyboardKeybinds.buttonDpadLeft, keyboardKeybinds.buttonDpadRight -> {
                        engine.postRunnable {
                            engine.inputter.onDpadButtonPressed(true)
                        }
                        consumed = true
                    }
                    keyboardKeybinds.buttonA -> {
                        engine.postRunnable {
                            engine.inputter.onAButtonPressed(true)
                        }
                        consumed = true
                    }
                }
            }
        }
        
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
        Gdx.input.isCursorCatched = true
    }

    override fun hide() {
        super.hide()
        Gdx.input.isCursorCatched = false
        main.inputMultiplexer.removeProcessor(inputProcessor)
    }

    override fun dispose() {
        // NOTE: container instance is disposed separately.
        // Additionally, the sound system is disposed in the container, so it doesn't have to be stopped.
        engine.endSignalReceived.removeListener(endSignalListener)
    }

    override fun getDebugString(): String {
        return """SoundSystem: paused=${soundSystem.isPaused}
---
${engine.getDebugString()}
---
${renderer.getDebugString()}
---
SideMode: ${sideMode?.javaClass?.name}${if (sideMode != null) ("\n" + sideMode.getDebugString()) else ""}
---
"""
    }

    inner class PauseBackground {
        private val random = Random()
        var seed: Int = 0
            private set
        private val hsv: FloatArray = FloatArray(3)
        var cycleSpeed: Float = 1 / 30f
        val triangleSlope: Float = 1 / 2f
        val topTriangleY: Float = 2 / 3f
        val botTriangleX: Float = 1 / 3f
        private val topColor: Color = Color.valueOf("232CDD")
        private val bottomColor: Color = Color.valueOf("d020a0")
        private val bgSquareTexReg: TextureRegion = TextureRegion(AssetRegistry.get<Texture>("pause_square"))

        init {
            randomizeSeed()
        }
        
        fun randomizeSeed() {
            seed = random.nextInt(Short.MAX_VALUE.toInt())
        }
        
        fun render(delta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
            val width = camera.viewportWidth
            val height = camera.viewportHeight

            if (cycleSpeed > 0f) {
                topColor.toHsv(hsv)
                hsv[0] = (hsv[0] - delta * cycleSpeed * 360f) % 360f
                topColor.fromHsv(hsv)
                bottomColor.toHsv(hsv)
                hsv[0] = (hsv[0] - delta * cycleSpeed * 360f) % 360f
                bottomColor.fromHsv(hsv)
            }

            batch.drawQuad(0f, 0f, bottomColor, width, 0f, bottomColor,
                    width, height, topColor, 0f, height, topColor)
            batch.setColor(1f, 1f, 1f, 1f)

            // Squares
            val squareCount = 90
            batch.setColor(1f, 1f, 1f, 0.65f)
            for (i in 0 until squareCount) {
                val alpha = i / squareCount.toFloat()
                val size = Interpolation.circleIn.apply(20f, 80f, alpha) * 1.5f
                val rotation = MathHelper.getSawtoothWave(System.currentTimeMillis() + (273L * alpha * 2).roundToLong(),
                        Interpolation.circleOut.apply(0.65f, 1.15f, alpha) * 0.75f) * (if (i % 2 == 0) -1 else 1)

                val yInterval = Interpolation.circleOut.apply(8f, 5f, alpha)
                val yAlpha = 1f - MathHelper.getSawtoothWave(System.currentTimeMillis() + (562L * alpha * 2).roundToLong(), yInterval)
                val x = MathUtils.lerp(width * -0.1f, width * 1.1f, yAlpha)
                val y = (width * 1.41421356f * (i + 23) * (alpha + seed) + (yAlpha * yInterval).roundToInt()) % (width * 1.25f)

                drawSquare(batch, x - size / 2, y - size / 2, rotation * 360f, size)
            }

            batch.setColor(1f, 1f, 1f, 1f)
        }

        private fun drawSquare(batch: SpriteBatch, x: Float, y: Float, rot: Float, size: Float) {
            val width = size
            val height = size
            batch.draw(bgSquareTexReg, x - width / 2, y - height / 2, width / 2, height / 2, width, height, 1f, 1f, rot)
        }
    }
    
}