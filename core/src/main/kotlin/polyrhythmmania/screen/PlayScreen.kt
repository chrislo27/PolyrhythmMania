package polyrhythmmania.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.transition.FadeIn
import paintbox.transition.FadeOut
import paintbox.transition.TransitionScreen
import paintbox.ui.SceneRoot
import paintbox.util.gdxutils.disposeQuietly
import paintbox.util.sumOfFloat
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.container.Container
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.input.InputType
import polyrhythmmania.screen.mainmenu.menu.TemporaryResultsMenu
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.world.EntityRod
import polyrhythmmania.world.render.WorldRenderer
import kotlin.math.absoluteValue
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

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        renderer.render(batch, engine)

        super.render(delta)
    }
    
    override fun renderUpdate() {
        super.renderUpdate()
        
        // DEBUG
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            val main = main
            val currentScreen = main.screen
            Gdx.app.postRunnable {
                val mainMenu = main.mainMenuScreen.prepareShow(doFlipAnimation = false)
                main.screen = TransitionScreen(main, currentScreen, mainMenu,
                        FadeOut(0.125f, Color(0f, 0f, 0f, 1f)), FadeIn(0.125f, Color(0f, 0f, 0f, 1f))).apply {
                    this.onEntryEnd = {
                        if (currentScreen is PlayScreen) {
                            currentScreen.dispose()
                        }
                    }
                }
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
        container.engine.autoInputs = false
        container.engine.inputter.areInputsLocked = false // FIXME may need better input locking mechanism later

        engine.endSignalReceived.addListener { endSignal ->
            if (endSignal.getOrCompute()) {
                Gdx.app.postRunnable {
                    isFinished = true
                }
            }
        }

        soundSystem.startRealtime()
        unpauseGame()
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
    
    private fun pauseGame() {
        isPaused = true
        soundSystem.setPaused(true)
        Gdx.input.isCursorCatched = false
        main.inputMultiplexer.removeProcessor(inputProcessor)
        main.inputMultiplexer.addProcessor(inputProcessor)
    }
    
    private fun unpauseGame() {
        isPaused = false
        soundSystem.setPaused(false)
        Gdx.input.isCursorCatched = true
        main.inputMultiplexer.removeProcessor(inputProcessor)
    }

    override fun keyDown(keycode: Int): Boolean {
        var consumed = false
        if (!isFinished) {
            if (isPaused) {
                when (keycode) {
                    Input.Keys.ESCAPE -> {
                        unpauseGame()
                        consumed = true
                    }
                }
            } else {
                val atSeconds = engine.seconds
                when (keycode) {
                    Input.Keys.ESCAPE -> {
                        pauseGame()
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

    override fun show() {
        super.show()
        startGame()
    }

    override fun hide() {
        super.hide()
        Gdx.input.isCursorCatched = false
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