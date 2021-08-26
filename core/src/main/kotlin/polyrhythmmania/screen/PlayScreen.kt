package polyrhythmmania.screen

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
import paintbox.binding.FloatVar
import paintbox.binding.Var
import paintbox.binding.VarChangedListener
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
import polyrhythmmania.container.Container
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.*
import polyrhythmmania.screen.results.ResultsScreen
import polyrhythmmania.sidemodes.AbstractEndlessMode
import polyrhythmmania.sidemodes.SideMode
import polyrhythmmania.sidemodes.endlessmode.EndlessPolyrhythm
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.world.EntityRodPR
import polyrhythmmania.world.render.WorldRenderer
import space.earlygrey.shapedrawer.ShapeDrawer
import java.util.*
import kotlin.math.*


class PlayScreen(
        main: PRManiaGame, val sideMode: SideMode?, val container: Container, val challenges: Challenges,
        val showResults: Boolean = true,
        val musicOffsetMs: Float = main.settings.musicOffsetMs.getOrCompute().toFloat(),
) : PRManiaScreen(main) {

    val timing: TimingProvider get() = container.timing
    val soundSystem: SoundSystem
        get() = container.soundSystem ?: error("PlayScreen requires a non-null SoundSystem in the Container")
    val engine: Engine get() = container.engine
    val renderer: WorldRenderer get() = container.renderer
    val batch: SpriteBatch = main.batch

    private val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    private val uiViewport: Viewport = FitViewport(uiCamera.viewportWidth, uiCamera.viewportHeight, uiCamera)
    private val sceneRoot: SceneRoot = SceneRoot(uiViewport)
    private val inputProcessor: InputProcessor = sceneRoot.inputSystem
    private val shapeDrawer: ShapeDrawer = ShapeDrawer(batch, PaintboxGame.paintboxSpritesheet.fill)
    private val pauseBg: PauseBackground by lazy { this.PauseBackground() }
    private val maxSelectionSize: Int = 3
    private val selectionIndex: Var<Int> = Var(0)
    private val panelAnimationValue: FloatVar = FloatVar(0f)
    private var activePanelAnimation: Animation? = null
    private val topPane: Pane
    private val bottomPane: Pane
    private val resumeLabel: TextLabel
    private val startOverLabel: TextLabel
    private val quitLabel: TextLabel
    private val optionLabels: List<TextLabel>

    private var isPaused: Boolean = false
    private var isFinished: Boolean = false

    private val keyboardKeybinds: InputKeymapKeyboard by lazy { main.settings.inputKeymapKeyboard.getOrCompute() }
    private val bgSquareTexReg: TextureRegion = TextureRegion(AssetRegistry.get<Texture>("pause_square"))
    
    private val endSignalListener: VarChangedListener<Boolean> = VarChangedListener {
        if (it.getOrCompute()) {
            Gdx.app.postRunnable {
                soundSystem.setPaused(true)
                container.world.entities.filterIsInstance<EntityRodPR>().forEach { rod ->
                    engine.inputter.submitInputsFromRod(rod)
                }
                if (showResults) {
                    transitionToResults()
                } else {
                    quitToMainMenu(false)
                }
            }
        }
    }

    init {
        var nextLayer: UIElement = sceneRoot
        fun addLayer(element: UIElement) {
            nextLayer += element
            nextLayer = element
        }
        addLayer(RectElement(Color(0f, 0f, 0f, 0f)))
        
        topPane = Pane().apply { 
            Anchor.TopLeft.configure(this, offsetY = {
                val h = bounds.height.useF()
                -h + panelAnimationValue.useF() * h
            })
            this.bindHeightToParent(multiplier = 0.3333f)
            this.bindWidthToSelfHeight(multiplier = 1f / pauseBg.triangleSlope)
            this.padding.set(Insets(36f, 0f, 64f, 0f))
        }
        nextLayer += topPane
        bottomPane = Pane().apply { 
            Anchor.BottomRight.configure(this, offsetY = {
                val h = bounds.height.useF()
                h + panelAnimationValue.useF() * -h
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

        leftVbox.temporarilyDisableLayouts {
            leftVbox += TextLabel(binding = { Localization.getVar("play.pause.title").use() }, font = main.fontPauseMenuTitle).apply {
                this.textColor.set(Color.WHITE)
                this.bounds.height.set(128f)
                this.renderAlign.set(Align.left)
            }
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
        val optionsBg = RectElement(transparentBlack).apply {
            Anchor.BottomRight.configure(this, offsetY = -80f, offsetX = -15f)
            this.bounds.width.set(275f + optionsBorderSize * 2)
            this.bounds.height.set(144f + optionsBorderSize * 2)
            this.border.set(Insets(optionsBorderSize))
            this.borderStyle.set(SolidBorder(transparentBlack).apply {
                this.roundedCorners.set(true)
            })
        }
        bottomPane += optionsBg
        
        fun addArrowImageNode(index: Int): ArrowNode {
            return ArrowNode(TextureRegion(AssetRegistry.get<PackedSheet>("ui_icon_editor")["arrow_pointer_finger"])).apply {
                Anchor.CentreLeft.configure(this, offsetY = 4f)
//                this.bindHeightToParent(multiplier = 1.5f)
                this.bounds.height.set(64f)
                this.bindWidthToSelfHeight()
                this.bounds.x.bind { -(bounds.width.useF() + optionsBorderSize + 2f) }
                this.visible.bind { selectionIndex.use() == index }
            }
        }

        val selectedLabelColor = Color(0f, 1f, 1f, 1f)
        val unselectedLabelColor = Color(1f, 1f, 1f, 1f)
        fun createTextLabelOption(localiz: String, index: Int, enabled: Boolean): TextLabel {
            return TextLabel(binding = { Localization.getVar(localiz).use() }, font = main.fontMainMenuMain).apply {
                Anchor.TopLeft.configure(this)
                this.disabled.set(!enabled)
                this.textColor.bind {
                    if (apparentDisabledState.use()) Color.GRAY else if (selectionIndex.use() == index) selectedLabelColor else unselectedLabelColor
                }
                this.bounds.height.set(48f)
                this.bgPadding.set(Insets(2f, 2f, 12f, 12f))
                this.renderAlign.set(Align.left)
                this.textAlign.set(TextAlign.LEFT)
                this += addArrowImageNode(index)
                this.setOnAction {
                    attemptPauseEntrySelection()
                }
                this.setOnHoverStart {
                    changeSelectionTo(index)
                }
            }
        }
        resumeLabel = createTextLabelOption(if (engine.autoInputs) "play.pause.resume.robotMode" else "play.pause.resume", 0, true)
        startOverLabel = createTextLabelOption("play.pause.startOver", 1, !(sideMode is EndlessPolyrhythm && sideMode.dailyChallenge != null))
        quitLabel = createTextLabelOption("play.pause.quitToMainMenu", 2, true)
        
        optionLabels = listOf(resumeLabel, startOverLabel, quitLabel)
        optionsBg += VBox().apply {
            this.spacing.set(0f)
            this.temporarilyDisableLayouts {
                this += resumeLabel
                this += startOverLabel
                this += quitLabel
            }
        }
    }

    init {
        engine.endSignalReceived.addListener(endSignalListener)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val batch = this.batch
        uiViewport.apply()
        renderer.render(batch)

        val camera = uiCamera
        batch.projectionMatrix = camera.combined
        batch.begin()

        if (isPaused) {
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

        if (!isPaused && timing is SimpleTimingProvider) {
            timing.seconds += Gdx.graphics.deltaTime
        }
    }

    private fun transitionToResults() {
        val inputter = engine.inputter
        val inputsHit = inputter.inputResults.count { it.inputScore != InputScore.MISS }
        val nInputs = max(inputter.totalExpectedInputs, inputter.minimumInputCount)
        val rawScore: Float = (if (nInputs <= 0) 0f else ((inputter.inputResults.map { it.inputScore }.sumOfFloat { inputScore ->
            inputScore.weight
        } / nInputs) * 100))
        val score: Int = rawScore.roundToInt().coerceIn(0, 100)
        
        val resultsText = container.resultsText
        val ranking = Ranking.getRanking(score)
        val leftResults = inputter.inputResults.filter { it.type == InputType.DPAD }
        val rightResults = inputter.inputResults.filter { it.type == InputType.A }
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
        }
        
        val scoreObj = Score(score, rawScore, inputsHit, nInputs,
                inputter.skillStarGotten.getOrCompute() && inputter.skillStarBeat.isFinite(), inputter.noMiss,
                challenges,
                resultsText.title ?: Localization.getValue("play.results.defaultTitle"),
                lines.first, lines.second,
                ranking, isNewHighScore
        )
        
        transitionAway(ResultsScreen(main, scoreObj, container, {
            PlayScreen(main, sideMode, container, challenges, showResults, musicOffsetMs)
        }, keyboardKeybinds), disposeContainer = false) {}
    }

    private inline fun transitionAway(nextScreen: Screen, disposeContainer: Boolean, action: () -> Unit) {
        isFinished = true
        main.inputMultiplexer.removeProcessor(inputProcessor)
        Gdx.input.isCursorCatched = false

        action.invoke()

        main.screen = TransitionScreen(main, this, nextScreen,
                FadeOut(0.5f, Color(0f, 0f, 0f, 1f)), FadeIn(0.125f, Color(0f, 0f, 0f, 1f))).apply {
            this.onEntryEnd = {
                this@PlayScreen.dispose()
                if (disposeContainer) {
                    container.disposeQuietly()
                }
            }
        }
    }

    fun prepareGameStart() {
        engine.inputter.areInputsLocked = engine.autoInputs
        engine.inputter.reset()
        renderer.resetAnimations()
        engine.musicOffsetMs = musicOffsetMs
        engine.removeActiveTextbox(false)
        engine.resetEndSignal()
        
        timing.seconds = -1f
        engine.seconds = timing.seconds
        val player = engine.soundInterface.getCurrentMusicPlayer(engine.musicData.beadsMusic)
        if (player != null) {
            val musicSample = player.musicSample
            musicSample.moveStartBuffer(0)
            engine.musicData.setMusicPlayerPositionToCurrentSec()
            player.pause(false)
        }

        soundSystem.startRealtime()
    }

    private fun pauseGame(playSound: Boolean) {
//        val endlessScore = engine.inputter.endlessScore
//        if (endlessScore.lives.getOrCompute() <= 0 && endlessScore.maxLives.getOrCompute() > 0) {
//            return // No pausing
//        }
        
        isPaused = true
        soundSystem.setPaused(true)
        Gdx.input.isCursorCatched = false
        main.inputMultiplexer.removeProcessor(inputProcessor)
        main.inputMultiplexer.addProcessor(inputProcessor)
        selectionIndex.set(0)
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

    private fun unpauseGame(playSound: Boolean) {
        isPaused = false
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

    fun resetAndStartOver(doWipeTransition: Boolean, playSound: Boolean = true) {
        if (playSound) {
            playMenuSound("sfx_menu_enter_game")
        }
        val thisScreen: PlayScreen = this
        val resetAction: () -> Unit = {
            challenges.applyToEngine(engine)
            val blocks = container.blocks.toList()
            engine.removeEvents(engine.events.toList())
            engine.addEvents(blocks.flatMap { it.compileIntoEvents() })
            container.world.resetWorld()
            container.world.tilesetPalette.applyTo(container.renderer.tileset)
            engine.soundInterface.clearAllNonMusicAudio()
            prepareGameStart()
            unpauseGame(false)
        }
        if (doWipeTransition) {
            main.screen = TransitionScreen(main, thisScreen, thisScreen,
                    WipeToColor(Color.BLACK.cpy(), 0.4f), WipeFromColor(Color.BLACK.cpy(), 0.4f)).apply {
                onEntryEnd = resetAction
                onStart = {
                    Gdx.input.isCursorCatched = true
                }
            }
        } else {
            resetAction()
        }
    }

    private fun attemptPauseEntrySelection() {
        val index = selectionIndex.getOrCompute()
        if (optionLabels[index].apparentDisabledState.getOrCompute()) return
        when (index) {
            0 -> { // Resume
                unpauseGame(true)
            }
            1 -> { // Start Over
                resetAndStartOver(true)
            }
            2 -> { // Quit to Main Menu
                quitToMainMenu(true)
            }
        }
    }

    private fun quitToMainMenu(playSound: Boolean) {
        val main = this@PlayScreen.main
        val currentScreen = main.screen
        Gdx.app.postRunnable {
            val mainMenu = main.mainMenuScreen.prepareShow(doFlipAnimation = true)
            main.screen = TransitionScreen(main, currentScreen, mainMenu,
                    FadeOut(0.25f, Color(0f, 0f, 0f, 1f)), null).apply {
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
    
    private fun changeSelectionTo(index: Int): Boolean {
        if (selectionIndex.getOrCompute() != index && !optionLabels[index].disabled.getOrCompute()) {
            selectionIndex.set(index)
            playMenuSound("sfx_menu_blip")
            return true
        }
        return false
    }

    override fun keyDown(keycode: Int): Boolean {
        var consumed = false
        if (!isFinished) {
            if (isPaused) {
                when (keycode) {
                    Input.Keys.ESCAPE, keyboardKeybinds.pause -> {
                        unpauseGame(true)
                        consumed = true
                    }
                    keyboardKeybinds.buttonDpadUp, keyboardKeybinds.buttonDpadDown -> {
                        if (optionLabels.any { !it.apparentDisabledState.getOrCompute() }) {
                            val currentIndex = selectionIndex.getOrCompute()
                            val incrementAmt = if (keycode == keyboardKeybinds.buttonDpadUp) -1 else 1
                            var increment = incrementAmt
                            var nextIndex: Int
                            do {
                                nextIndex = (selectionIndex.getOrCompute() + increment + maxSelectionSize) % maxSelectionSize
                                if (changeSelectionTo(nextIndex)) {
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
        if (!isFinished) {
            if (!isPaused)  {
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

    private fun playMenuSound(id: String, volume: Float = 1f, pitch: Float = 1f, pan: Float = 0f): Pair<Sound, Long> {
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
        // NOTE: container instance is disposed separately
        engine.endSignalReceived.removeListener(endSignalListener)
    }

    override fun getDebugString(): String {
        return """SoundSystem: paused=${soundSystem.isPaused}
TimingBead: ${soundSystem.seconds}
---
${engine.getDebugString()}
---
${renderer.getDebugString()}
---
SideMode: ${sideMode?.javaClass?.canonicalName}${if (sideMode != null) ("\n" + sideMode.getDebugString()) else ""}
---
${sceneRoot.mainLayer.lastHoveredElementPath.map { it.javaClass.simpleName }}
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

    class ArrowNode(val tex: TextureRegion) : UIElement() {
        override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
            val renderBounds = this.contentZone
            val x = renderBounds.x.get() + originX
            val y = originY - renderBounds.y.get()
            val w = renderBounds.width.get()
            val h = renderBounds.height.get()
            val offsetXMax = (w * 0.35f)
            val offsetX = (MathHelper.getSawtoothWave(1f) * 4f).coerceIn(0f, 1f) * offsetXMax
            batch.draw(tex, x + offsetX - offsetXMax, y - h,
                    0.5f * w, 0.5f * h,
                    w, h, 1f, 1f, 0f)
        }
    }
}