package polyrhythmmania.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import io.github.chrislo27.paintbox.ui.SceneRoot
import io.github.chrislo27.paintbox.ui.UIElement
import io.github.chrislo27.paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen


class EditorScreen(main: PRManiaGame, val debugMode: Boolean = false) : PRManiaScreen(main) {

    val batch: SpriteBatch = main.batch
    val editor: Editor = Editor(main)
    private val sceneRoot: SceneRoot = editor.sceneRoot
    private val processor: InputProcessor = editor

    init {
        editor.resize()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        super.render(delta)

        editor.render(delta, batch)
    }

    override fun renderUpdate() {
        super.renderUpdate()
        
        editor.renderUpdate()

        // DEBUG resets editor scene
        if (debugMode && Gdx.input.isKeyJustPressed(Input.Keys.R) && false) {
            val thisEditorScreen: EditorScreen = this
            Gdx.app.postRunnable { 
                main.screen = EditorScreen(main, debugMode = true)
                Gdx.app.postRunnable {
                    thisEditorScreen.dispose()
                }
            }
        }
    }
    
    private fun countChildren(element: UIElement): Int {
        return 1 + element.children.size + element.children.sumBy { countChildren(it) }
    }

    override fun getDebugString(): String {
        return """${editor.getDebugString()}

"""
    }

    override fun show() {
        super.show()
        main.inputMultiplexer.removeProcessor(processor)
        main.inputMultiplexer.addProcessor(processor)
    }

    override fun hide() {
        super.hide()
        main.inputMultiplexer.removeProcessor(processor)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        editor.resize()
    }
    
    override fun dispose() {
        editor.dispose()
    }
}