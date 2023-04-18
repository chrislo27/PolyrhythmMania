package polyrhythmmania.screen.play

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import paintbox.binding.BooleanVar
import paintbox.binding.VarChangedListener
import paintbox.transition.FadeToOpaque
import paintbox.transition.FadeToTransparent
import paintbox.transition.TransitionScreen
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame
import polyrhythmmania.container.Container
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.InputCalibration
import polyrhythmmania.engine.ResultFlag
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.engine.input.InputType
import polyrhythmmania.gamemodes.GameMode
import polyrhythmmania.soundsystem.SimpleTimingProvider
import polyrhythmmania.soundsystem.SoundSystem
import polyrhythmmania.soundsystem.TimingProvider
import polyrhythmmania.statistics.PlayTimeType
import polyrhythmmania.world.EntityRodPR
import polyrhythmmania.world.render.WorldRendererWithUI
import kotlin.math.max


/**
 * A generic play screen for [Engine]-based gameplay.
 */
abstract class AbstractEnginePlayScreen(
        main: PRManiaGame, playTimeType: PlayTimeType?,

        val container: Container,
        val challenges: Challenges, val inputCalibration: InputCalibration,
        
        val gameMode: GameMode?
) : AbstractPlayScreen(main, playTimeType) {

    companion object; // Used for early init
    
    val timing: TimingProvider get() = container.timing
    val soundSystem: SoundSystem
        get() = container.soundSystem ?: error("${this::javaClass.name} requires a non-null SoundSystem in the Container")
    val engine: Engine get() = container.engine
    val worldRenderer: WorldRendererWithUI get() = container.renderer
    
    val shouldUpdateTiming: BooleanVar = BooleanVar(true)
    
    protected var goingToResults: Boolean = false

    private val endSignalListener: VarChangedListener<Boolean> = VarChangedListener {
        if (it.getOrCompute()) {
            Gdx.app.postRunnable {
                onEndSignalFired()
            }
        }
    }
    private val resultFlagListener: VarChangedListener<ResultFlag> = VarChangedListener {
        val flag = it.getOrCompute()
        if (flag != ResultFlag.None) {
            Gdx.app.postRunnable {
                onResultFlagChanged(flag)
            }
        }
    }

    init {
        engine.endSignalReceived.addListener(endSignalListener)
        engine.resultFlag.addListener(resultFlagListener)
    }
    

    override fun renderGameplay(delta: Float) {
        worldRenderer.render(batch)
    }

    override fun initializeGameplay() {
        engine.inputCalibration = this.inputCalibration
        
        // Set timing and music
        timing.seconds = -(getSecondsToDelayAtStart() + max(0f, this.inputCalibration.audioOffsetMs / 1000f))
        engine.seconds = timing.seconds
        val player = engine.soundInterface.getCurrentMusicPlayer(engine.musicData.beadsMusic)
        if (player != null) { // Set music player position
            val musicSample = player.musicSample
            musicSample.moveStartBuffer(0)
            engine.musicData.setMusicPlayerPositionToCurrentSec()
            player.pause(false)
        }
        soundSystem.startRealtime() // Does nothing if already started
        
        container.resetMutableState()
        
        challenges.applyToEngine(engine)
    }

    override fun renderUpdate() {
        super.renderUpdate()

        if (!isPaused.get() && timing is SimpleTimingProvider && shouldUpdateTiming.get()) {
            timing.seconds += Gdx.graphics.deltaTime
            gameMode?.renderUpdate()
        }
    }
    
    protected open fun getSecondsToDelayAtStart(): Float = 1f
    
    protected fun submitAllRodPRInputs() {
        container.world.entities.filterIsInstance<EntityRodPR>().forEach { rod ->
            engine.inputter.submitInputsFromRod(rod)
        }
    }

    /**
     * Will be triggered in the gdx main thread.
     */
    protected open fun onEndSignalFired() {
        soundSystem.setPaused(true)
        submitAllRodPRInputs()
    }

    /**
     * Will be triggered in the gdx main thread.
     */
    protected open fun onResultFlagChanged(flag: ResultFlag) {
    }
    
    
    protected fun quitToScreen(
        targetScreen: Screen? = main.mainMenuScreen.prepareShow(doFlipAnimation = true),
        longStartTransition: Boolean = false
    ) {
        val main = this.main
        val currentScreen = this
        Gdx.app.postRunnable {
            main.screen = TransitionScreen(
                main, currentScreen, targetScreen,
                FadeToOpaque(if (!longStartTransition) 0.25f else 2f, Color(0f, 0f, 0f, 1f)),
                FadeToTransparent(0.125f, Color(0f, 0f, 0f, 1f))
            ).apply {
                this.onEntryEnd = {
                    currentScreen.dispose()
                    container.disposeQuietly()
                }
            }
        }
    }
    
    override fun pauseGame(playSound: Boolean) {
        super.pauseGame(playSound)
        
        soundSystem.setPaused(true)
    }

    override fun unpauseGame(playSound: Boolean) {
        super.unpauseGame(playSound)
        
        val player = engine.soundInterface.getCurrentMusicPlayer(engine.musicData.beadsMusic)
        if (player != null) {
            engine.musicData.setMusicPlayerPositionToCurrentSec()
            player.pause(false)
        }
        soundSystem.setPaused(false)
    }


    override fun shouldCatchCursor(): Boolean = true
    
    override fun uncatchCursorOnHide(): Boolean {
        return !goingToResults
    }
    
    override fun keyDown(keycode: Int): Boolean {
        var consumed = false
        if (main.screen === this) {
            if (!isPaused.get()) {
                when (keycode) {
                    keyboardKeybinds.buttonDpadUp -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(false, InputType.DPAD_UP)
                        }
                        consumed = true
                    }
                    keyboardKeybinds.buttonDpadDown -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(false, InputType.DPAD_DOWN)
                        }
                        consumed = true
                    }
                    keyboardKeybinds.buttonDpadLeft -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(false, InputType.DPAD_LEFT)
                        }
                        consumed = true
                    }
                    keyboardKeybinds.buttonDpadRight -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(false, InputType.DPAD_RIGHT)
                        }
                        consumed = true
                    }
                    keyboardKeybinds.buttonA -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(false, InputType.A)
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
                    keyboardKeybinds.buttonDpadUp -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(true, InputType.DPAD_UP)
                        }
                        consumed = true
                    }
                    keyboardKeybinds.buttonDpadDown -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(true, InputType.DPAD_DOWN)
                        }
                        consumed = true
                    }
                    keyboardKeybinds.buttonDpadLeft -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(true, InputType.DPAD_LEFT)
                        }
                        consumed = true
                    }
                    keyboardKeybinds.buttonDpadRight -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(true, InputType.DPAD_RIGHT)
                        }
                        consumed = true
                    }
                    keyboardKeybinds.buttonA -> {
                        engine.postRunnable {
                            engine.inputter.onButtonPressed(true, InputType.A)
                        }
                        consumed = true
                    }
                }
            }
        }

        return consumed || super.keyUp(keycode)
    }

    override fun _dispose() {
        // NOTE: container instance is disposed separately.
        // Additionally, the sound system is disposed in the container, so it doesn't have to be stopped.
        engine.endSignalReceived.removeListener(endSignalListener)
        engine.resultFlag.removeListener(resultFlagListener)
    }

    override fun getDebugString(): String {
        return super.getDebugString() + """---
SoundSystem: paused=${soundSystem.isPaused}
---
${engine.getDebugString()}
---
${worldRenderer.getDebugString()}
---
SideMode: ${gameMode?.javaClass?.name}${if (gameMode != null) ("\n" + gameMode.getDebugString()) else ""}
"""
    }
    
}
