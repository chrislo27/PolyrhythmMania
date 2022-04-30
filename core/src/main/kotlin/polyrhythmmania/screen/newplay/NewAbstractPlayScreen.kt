package polyrhythmmania.screen.newplay

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.Viewport
import paintbox.PaintboxGame
import paintbox.binding.BooleanVar
import paintbox.binding.ReadOnlyBooleanVar
import paintbox.binding.Var
import paintbox.registry.AssetRegistry
import paintbox.ui.SceneRoot
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.engine.input.InputKeymapKeyboard
import polyrhythmmania.screen.play.PauseOption
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.statistics.PlayTimeType
import space.earlygrey.shapedrawer.ShapeDrawer


/**
 * A generic play screen that can be paused.
 */
abstract class NewAbstractPlayScreen(
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

    protected val pauseOptions: Var<List<PauseOption>> = Var(emptyList())
    protected val selectedPauseOption: Var<PauseOption?> = Var(null)

    
    protected abstract fun shouldCatchCursor(): Boolean

    /**
     * Called to initialize gameplay. This will be called before entering this screen for the first time,
     * and will be called when starting over.
     * @see resetAndUnpause
     */
    protected abstract fun initializeGameplay()

    /**
     * Called when starting over.
     * @see startOver
     */
    protected abstract fun onStartOver()

    /**
     * Call to dispose this screen. This should be called when leaving this screen and not returning, INCLUDING
     * if going to results. 
     */
    abstract override fun dispose()
    
    /**
     * Reinitializes the game and unpauses (without playing a sound).
     */
    fun resetAndUnpause() {
        initializeGameplay()
        unpauseGame(false)
    }
    
    /**
     * Starts over the gameplay. Calls [onStartOver] and [resetAndUnpause].
     */
    fun startOver() {
        onStartOver()
        resetAndUnpause()
    }


    protected abstract fun renderGameplay(delta: Float)

    /**
     * Called after rendering gameplay. Note: the [batch] has already been started.
     *
     * Use to render the pause screen.
     */
    protected abstract fun renderAfterGameplay(delta: Float)
    
    
    // Note: May remove final modifier if needed later down the road
    final override fun render(delta: Float) {
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

        renderAfterGameplay(delta)

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

        main.inputMultiplexer.removeProcessor(pauseMenuInputProcessor)
        main.inputMultiplexer.addProcessor(pauseMenuInputProcessor)
        if (shouldCatchCursor()) {
            Gdx.input.isCursorCatched = false
        }
        
        selectedPauseOption.set(pauseOptions.getOrCompute().firstOrNull())
        
        if (playSound) {
            playMenuSound("sfx_pause_enter")
        }
    }

    /**
     * Unpauses the game.
     */
    protected open fun unpauseGame(playSound: Boolean) {
        (isPaused as BooleanVar).set(false)

        main.inputMultiplexer.removeProcessor(pauseMenuInputProcessor)
        if (shouldCatchCursor()) {
            Gdx.input.isCursorCatched = true
        }
        
        if (playSound) {
            playMenuSound("sfx_pause_exit")
        }
    }
    

    override fun keyDown(keycode: Int): Boolean {
        var consumed = false
        if (main.screen === this) {
            if (isPaused.get()) { // Pause menu selection
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
                        attemptSelectCurrentPauseOption()
                        consumed = true
                    }
                }
            } else {
                when (keycode) { // Open pause menu
                    Input.Keys.ESCAPE, keyboardKeybinds.pause -> {
                        pauseGame(true)
                        consumed = true
                    }
                }
            }
        }

        return consumed || super.keyDown(keycode)
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


    protected fun attemptSelectCurrentPauseOption() {
        val pauseOp = selectedPauseOption.getOrCompute()
        if (pauseOp != null && pauseOp.enabled) {
            pauseOp.action()
        }
    }

    protected fun changeSelectionTo(option: PauseOption): Boolean {
        if (selectedPauseOption.getOrCompute() != option && option.enabled) {
            selectedPauseOption.set(option)
            playMenuSound("sfx_menu_blip")
            return true
        }
        return false
    }

    protected fun playMenuSound(id: String, volume: Float = 1f, pitch: Float = 1f, pan: Float = 0f): Pair<Sound, Long> {
        val sound: Sound = AssetRegistry[id]
        val menuSFXVol = main.settings.menuSfxVolume.getOrCompute() / 100f
        val soundID = sound.play(menuSFXVol * volume, pitch, pan)
        return sound to soundID
    }

    override fun getDebugString(): String {
        return "Paused: ${isPaused.get()}\n"
    }
    
}