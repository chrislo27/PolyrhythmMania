package polyrhythmmania.storymode.test.gamemode

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.PRManiaGame


class TestStoryNoOpEndlessUIGameMode(main: PRManiaGame) : TestStoryGameMode(main) {
    
    init {
        val endlessScore = engine.modifiers.endlessScore
        endlessScore.enabled.set(true)
        endlessScore.maxLives.set(5)
    }

    override fun initialize() {
        // NO-OP
        println()
    }

    override fun renderUpdate() {
        super.renderUpdate()

        val endlessScore = engine.modifiers.endlessScore
        if (Gdx.input.isKeyJustPressed(Keys.C)) { // Increment/reset score
            if (Gdx.input.isShiftDown()) {
                endlessScore.score.set(0)
            } else {
                endlessScore.score.incrementAndGet()
            }
        }
        if (Gdx.input.isKeyJustPressed(Keys.L)) { // Decrement/reset lives
            if (Gdx.input.isShiftDown()) {
                endlessScore.lives.set(endlessScore.startingLives.get())
            } else {
                if (endlessScore.lives.get() > 0) {
                    endlessScore.lives.decrementAndGet()
                }
            }
        }
    }
}