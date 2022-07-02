package polyrhythmmania.storymode.test.gamemode

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import paintbox.util.gdxutils.isControlDown
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

        /*
        C - increment score
        Shift+C - set score to 0
        
        L - lose a life
        Shift+L - add a life
        
        Ctrl+L - Add 1 max life
        Ctrl+Shift+L - Reduce max lives by 1
         */
        val endlessScore = engine.modifiers.endlessScore
        if (Gdx.input.isKeyJustPressed(Keys.C)) { // Increment/reset score
            if (Gdx.input.isShiftDown()) {
                endlessScore.score.set(0)
            } else {
                endlessScore.score.incrementAndGet()
            }
        }
        if (Gdx.input.isKeyJustPressed(Keys.L)) { // Decrement/reset lives
            if (Gdx.input.isControlDown()) {
                val amt = if (Gdx.input.isShiftDown()) -1 else 1
                endlessScore.maxLives.set((endlessScore.maxLives.get() + amt).coerceIn(1, 5))
                endlessScore.lives.set(endlessScore.maxLives.get())
            } else {
                if (Gdx.input.isShiftDown()) {
                    endlessScore.lives.set((endlessScore.lives.get() + 1).coerceIn(0, endlessScore.maxLives.get()))
                } else {
                    if (endlessScore.lives.get() > 0) {
                        endlessScore.lives.decrementAndGet()
                    }
                }
            }
        }
    }
}