package io.github.chrislo27.paintbox.tests

import com.badlogic.gdx.Gdx
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.PaintboxScreen
import io.github.chrislo27.paintbox.PaintboxSettings
import io.github.chrislo27.paintbox.ResizeAction
import io.github.chrislo27.paintbox.logging.Logger
import io.github.chrislo27.paintbox.util.Version
import io.github.chrislo27.paintbox.util.WindowSize
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
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