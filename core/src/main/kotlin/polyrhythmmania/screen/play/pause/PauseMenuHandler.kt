package polyrhythmmania.screen.play.pause

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import paintbox.binding.ReadOnlyBooleanVar
import paintbox.binding.Var
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.input.InputKeymapKeyboard
import polyrhythmmania.screen.play.AbstractPlayScreen


abstract class PauseMenuHandler(val screen: AbstractPlayScreen) : Disposable {
    
    protected val main: PRManiaGame get() = screen.main
    protected val batch: SpriteBatch get() = screen.batch
    protected val keyboardKeybinds: InputKeymapKeyboard get() = screen.keyboardKeybinds
    protected val isPaused: ReadOnlyBooleanVar get() = screen.isPaused
    protected val pauseOptions: Var<List<PauseOption>> get() = screen.pauseOptions
    protected val selectedPauseOption: Var<PauseOption?> get() = screen.selectedPauseOption
    
    
    abstract fun renderAfterGameplay(delta: Float, camera: OrthographicCamera)
    
    open fun onPaused() {}
    open fun onUnpaused() {}
    open fun onStartOver() {}
    
}