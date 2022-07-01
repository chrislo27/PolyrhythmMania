package polyrhythmmania.storymode.test.gamemode

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.ResultFlag


class TestStoryNoOpLivesUIGameMode(main: PRManiaGame) : TestStoryGameMode(main) {

    init {
        val livesMode = engine.modifiers.livesMode
        livesMode.enabled.set(true)
        livesMode.maxLives.set(3)
    }

    override fun initialize() {
        // NO-OP
        println()
    }

    override fun renderUpdate() {
        super.renderUpdate()

        val livesMode = engine.modifiers.livesMode
        
        if (Gdx.input.isKeyJustPressed(Keys.C)) { // Increment/decrement max lives
            val amt = if (Gdx.input.isShiftDown()) -1 else 1
            val newMaxLives = livesMode.maxLives.get() + amt
            if (newMaxLives in 1..10) {
                livesMode.maxLives.set(newMaxLives)
                livesMode.lives.set(newMaxLives)
                engine.resultFlag.set(ResultFlag.NONE)
            }
        }
        
        if (Gdx.input.isKeyJustPressed(Keys.F)) { // Reduce life by one
            livesMode.loseALife(engine.inputter)
        }
    }
}