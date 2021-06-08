package polyrhythmmania.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import paintbox.registry.AssetRegistry
import paintbox.transition.FadeIn
import paintbox.transition.FadeOut
import paintbox.transition.TransitionScreen
import paintbox.ui.Pane
import paintbox.ui.SceneRoot
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.control.Button
import paintbox.ui.control.ButtonSkin
import paintbox.ui.control.TextLabel
import paintbox.ui.element.RectElement
import paintbox.ui.layout.VBox
import paintbox.ui.skin.Skin
import paintbox.ui.skin.SkinFactory
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.sumOfFloat
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.container.Container
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.InputType
import polyrhythmmania.screen.mainmenu.menu.TemporaryResultsMenu
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.world.EntityRod
import polyrhythmmania.world.render.WorldRenderer
import kotlin.math.roundToInt


class PlayScreen(main: PRManiaGame, val container: Container)
    : PRManiaScreen(main) {

    val timing: TimingProvider get() = container.timing
    val soundSystem: SoundSystem get() = container.soundSystem ?: error("PlayScreen requires a non-null SoundSystem in the Container")
    val engine: Engine get() = container.engine
    val renderer: WorldRenderer get() = container.renderer
    val batch: SpriteBatch = main.batch
    
    private val uiCamera: OrthographicCamera = OrthographicCamera().apply {
        this.setToOrtho(false, 1280f, 720f)
        this.update()
    }
    private val sceneRoot: SceneRoot = SceneRoot(uiCamera)
    private val inputProcessor: InputProcessor = sceneRoot.inputSystem
    
    private var isPaused: Boolean = false
    private var isFinished: Boolean = false

    init {
        var nextLayer: UIElement = sceneRoot
        fun addLayer(element: UIElement) {
            nextLayer += element
            nextLayer = element
        }
        addLayer(RectElement(Color(0f, 0f, 0f, 0.5f)))
        
        val vbox = VBox().apply { 
            spacing.set(16f)
            this.padding.set(Insets(64f))
        }
        addLayer(vbox)
        
        vbox.temporarilyDisableLayouts { 
            vbox += TextLabel("Pause Menu", font = main.fontMainMenuHeading).apply { 
                this.textColor.set(Color.BLACK)
                this.bgPadding.set(Insets(16f))
                this.backgroundColor.set(Color(1f, 1f, 1f, 1f))
                this.renderBackground.set(true)
                this.bounds.height.set(64f)
                this.renderAlign.set(Align.left)
            }
            vbox += TextLabel("(temporary menu!)", font = main.fontMainMenuThin).apply {
                this.textColor.set(Color.BLACK)
                this.bgPadding.set(Insets(16f))
                this.backgroundColor.set(Color(1f, 1f, 1f, 1f))
                this.renderBackground.set(true)
                this.bounds.height.set(32f)
                this.renderAlign.set(Align.left)
            }
            vbox += Pane().apply { this.bounds.height.set(64f) }
            val skinFactory = SkinFactory { element: Button -> 
                ButtonSkin(element).apply { 
                    this.roundedRadius.set(8)
                } as Skin<Button>
            }
            vbox += Button("Resume", font = main.fontMainMenuMain).apply {
                this.bounds.width.set(250f)
                this.bounds.height.set(64f)
                this.skinFactory.set(skinFactory)
                this.setOnAction { 
                    unpauseGame(true)
                }
            }
            vbox += Button("Start Over", font = main.fontMainMenuMain).apply {
                this.bounds.width.set(250f)
                this.bounds.height.set(64f)
                this.skinFactory.set(skinFactory)
                this.setOnAction {
                    resetAndStartOver()
                }
            }
            vbox += Button("Quit to Main Menu", font = main.fontMainMenuMain).apply {
                this.bounds.width.set(250f)
                this.bounds.height.set(64f)
                this.skinFactory.set(skinFactory)
                this.setOnAction {
                    val main = this@PlayScreen.main
                    val currentScreen = main.screen
                    Gdx.app.postRunnable {
                        val mainMenu = main.mainMenuScreen.prepareShow(doFlipAnimation = true)
                        main.screen = TransitionScreen(main, currentScreen, mainMenu,
                                FadeOut(0.25f, Color(0f, 0f, 0f, 1f)), null).apply {
                            this.onEntryEnd = {
                                if (currentScreen is PlayScreen) {
                                    currentScreen.dispose()
                                }
                            }
                        }
                    }
                    Gdx.app.postRunnable {
                        playMenuSound("sfx_pause_exit")
                    }
                }
            }
        }
    }
    
    init {
        engine.endSignalReceived.addListener {
            Gdx.app.postRunnable { 
                soundSystem.setPaused(true)
                container.world.entities.filterIsInstance<EntityRod>().forEach { rod ->
                    engine.inputter.submitInputsFromRod(rod)
                }
                transitionToResults()
            }
        }
    }

    fun startGame() {
        timing.seconds = 0f
        val player = engine.soundInterface.getCurrentMusicPlayer(engine.musicData.beadsMusic)
        if (player != null) {
            engine.musicData.setPlayerPositionToCurrentSec()
            player.pause(false)
        }
        engine.autoInputs = false
        engine.inputter.areInputsLocked = false // FIXME may need better input locking mechanism later
        engine.inputter.clearInputs()
        engine.resetEndSignal()

        engine.endSignalReceived.addListener { endSignal ->
            if (endSignal.getOrCompute()) {
                Gdx.app.postRunnable {
                    isFinished = true
                }
            }
        }

        soundSystem.startRealtime()
        unpauseGame(false)
    }
    
    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val batch = this.batch
        renderer.render(batch, engine)
        
        batch.projectionMatrix = uiCamera.combined
        batch.begin()
        
        if (isPaused) {
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
        val nInputs = inputter.totalExpectedInputs
        val score = if (nInputs <= 0) 0f else ((inputter.inputResults.map { it.inputScore }.sumOfFloat { inputScore ->
            inputScore.weight
        } / nInputs) * 100)
        val results = TemporaryResultsMenu.Results(nInputs, score.roundToInt().coerceIn(0, 100), inputter.inputResults)

        val mainMenu = main.mainMenuScreen.prepareShow(doFlipAnimation = true)
        val menuCol = mainMenu.menuCollection
        val tmpResultsMenu = TemporaryResultsMenu(menuCol, results)
        menuCol.addMenu(tmpResultsMenu)
        menuCol.pushNextMenu(tmpResultsMenu, instant = true)
        transitionAway(mainMenu) {}
    }

    private inline fun transitionAway(nextScreen: Screen, action: () -> Unit) {
        isFinished = true
        main.inputMultiplexer.removeProcessor(inputProcessor)
        Gdx.input.isCursorCatched = false

        action.invoke()

        main.screen = TransitionScreen(main, this, nextScreen,
                FadeOut(0.5f, Color(0f, 0f, 0f, 1f)), FadeIn(0.125f, Color(0f, 0f, 0f, 1f))).apply {
            this.onEntryEnd = {
                this@PlayScreen.dispose()
            }
        }
    }
    
    private fun pauseGame(playSound: Boolean) {
        isPaused = true
        soundSystem.setPaused(true)
        Gdx.input.isCursorCatched = false
        main.inputMultiplexer.removeProcessor(inputProcessor)
        main.inputMultiplexer.addProcessor(inputProcessor)
        if (playSound) {
            playMenuSound("sfx_pause_enter")
        }
    }
    
    private fun unpauseGame(playSound: Boolean) {
        isPaused = false
        soundSystem.setPaused(false)
        Gdx.input.isCursorCatched = true
        main.inputMultiplexer.removeProcessor(inputProcessor)
        if (playSound) {
            playMenuSound("sfx_pause_exit")
        }
    }
    
    private fun resetAndStartOver(playSound: Boolean = true) {
        val blocks = container.blocks.toList()
        engine.removeEvents(engine.events.toList())
        engine.addEvents(blocks.flatMap { it.compileIntoEvents() })
        container.world.resetWorld()
        Gdx.app.postRunnable {
            if (playSound) {
                playMenuSound("sfx_menu_enter_game")
            }
            startGame()
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        var consumed = false
        if (!isFinished) {
            if (isPaused) {
                when (keycode) {
                    Input.Keys.ESCAPE -> {
                        unpauseGame(true)
                        consumed = true
                    }
                }
            } else {
                val atSeconds = engine.seconds
                when (keycode) {
                    Input.Keys.ESCAPE -> {
                        pauseGame(true)
                        consumed = true
                    }
                    // TODO use keybinds!
                    Input.Keys.D, Input.Keys.W, Input.Keys.S, Input.Keys.A -> {
                        engine.postRunnable {
                            engine.inputter.onInput(InputType.DPAD, atSeconds)
                        }
                    }
                    Input.Keys.J -> {
                        engine.postRunnable {
                            engine.inputter.onInput(InputType.A, atSeconds)
                        }
                    }
                }
            }
        }
        
        return consumed || super.keyDown(keycode)
    }
    
    private fun playMenuSound(id: String, volume: Float = 1f, pitch: Float = 1f, pan: Float = 0f): Pair<Sound, Long> {
        val sound: Sound = AssetRegistry[id]
        val menuSFXVol = main.settings.menuSfxVolume.getOrCompute() / 100f
        val soundID = sound.play(menuSFXVol * volume, pitch, pan)
        return sound to soundID
    }

    override fun show() {
        super.show()
        startGame()
    }

    override fun hide() {
        super.hide()
        Gdx.input.isCursorCatched = false
        main.inputMultiplexer.removeProcessor(inputProcessor)
    }

    override fun dispose() {
        container.disposeQuietly()
    }
    
    override fun getDebugString(): String {
        return """SoundSystem: paused=${soundSystem.isPaused}
TimingBead: ${soundSystem.seconds}
---
${engine.getDebugString()}
---
${renderer.getDebugString()}
"""

    }
}