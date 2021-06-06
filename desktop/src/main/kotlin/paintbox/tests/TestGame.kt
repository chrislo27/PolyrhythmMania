package paintbox.tests

import com.badlogic.gdx.Gdx
import paintbox.PaintboxGame
import paintbox.PaintboxScreen
import paintbox.PaintboxSettings
import paintbox.ResizeAction
import paintbox.logging.Logger
import paintbox.util.Version
import paintbox.util.WindowSize
import paintbox.util.gdxutils.fillRect
import java.io.File


internal abstract class TestPbScreen(override val main: TestGame) : PaintboxScreen()

internal class TestGame(paintboxSettings: PaintboxSettings)
    : PaintboxGame(paintboxSettings) {

    override fun getTitle(): String {
        return "TestGame"
    }

    override fun create() {
        super.create()
        this.screen = TestScreen(this)
    }

}

internal class TestScreen(main: TestGame) : TestPbScreen(main) {

    override fun render(delta: Float) {
        val batch = main.batch
        batch.begin()
        batch.setColor(1f, 1f, 1f, 1f)
        batch.fillRect(0f, 0f, Gdx.graphics.width + 0f, Gdx.graphics.height + 0f)
        
        batch.end()
        super.render(delta)
    }

    override fun dispose() {
    }
}