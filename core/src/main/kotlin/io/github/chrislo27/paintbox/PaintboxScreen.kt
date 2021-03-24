package io.github.chrislo27.paintbox

import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.Screen
import com.badlogic.gdx.math.Matrix4


abstract class PaintboxScreen : Screen, InputProcessor {
    
    companion object {
        private val TMP_MATRIX = Matrix4()
    }
    
    abstract val main: PaintboxGame

    override fun render(delta: Float) {
//        run {
//            val stage = this.stage ?: return@run
//            val batch = main.batch
//
//            batch.begin()
//            if (stage.visible)
//                stage.render(this as SELF, batch, main.shapeRenderer)
//            if (Paintbox.stageOutlines != Paintbox.StageOutlineMode.NONE) {
//                val old = batch.packedColor
//                TMP_MATRIX.set(batch.projectionMatrix)
//                batch.projectionMatrix = stage.camera.combined
//                batch.setColor(0f, 1f, 0f, 1f)
//                stage.drawOutline(batch, stage.camera, 1f, Paintbox.stageOutlines == Paintbox.StageOutlineMode.ONLY_VISIBLE)
//                batch.packedColor = old
//                batch.projectionMatrix = TMP_MATRIX
//            }
//            batch.end()
//        }
    }

    open fun renderUpdate() {
//        stage?.frameUpdate(this as SELF)
    }

    open fun getDebugString(): String? {
        return null
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun show() {
//        stage?.updatePositions()
        main.inputMultiplexer.removeProcessor(this)
        main.inputMultiplexer.addProcessor(this)
    }

    override fun hide() {
        main.inputMultiplexer.removeProcessor(this)
    }

    open fun showTransition() {

    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
//        return stage?.touchUp(screenX, screenY, pointer, button) ?: false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
//        return stage?.mouseMoved(screenX, screenY) ?: false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
//        return stage?.keyTyped(character) ?: false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return false
//        return stage?.scrolled(amountX, amountY) ?: false
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
//        return stage?.keyUp(keycode) ?: false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
//        return stage?.touchDragged(screenX, screenY, pointer) ?: false
    }

    override fun keyDown(keycode: Int): Boolean {
        return false
//        return stage?.keyDown(keycode) ?: false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
//        return stage?.touchDown(screenX, screenY, pointer, button) ?: false
    }
}