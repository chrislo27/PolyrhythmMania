package polyrhythmmania.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import paintbox.ui.SceneRoot
import paintbox.ui.UIElement
import paintbox.util.gdxutils.disposeQuietly
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen


class EditorScreen(main: PRManiaGame, val debugMode: Boolean = false) : PRManiaScreen(main) {

    val batch: SpriteBatch = main.batch
    val editor: Editor = Editor(main)
    private val sceneRoot: SceneRoot = editor.sceneRoot
    private val processor: InputProcessor = editor

    private var lastWindowListener: Lwjgl3WindowListener? = null
    
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
        if (debugMode && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            val thisEditorScreen: EditorScreen = this
            Gdx.app.postRunnable {
                main.screen = null
                thisEditorScreen.dispose()
                main.screen = EditorScreen(main, debugMode = true)
            }
        }
    }
    
    private fun countChildren(element: UIElement): Int {
        return 1 + element.children.size + element.children.sumOf { countChildren(it) }
    }

    override fun getDebugString(): String {
        return """${editor.getDebugString()}

"""
    }

    override fun show() {
        super.show()
        main.inputMultiplexer.removeProcessor(processor)
        main.inputMultiplexer.addProcessor(processor)
        
        // TODO: should the window listener be wrapped since only the filesDropped function is used? Then other listeners can retain their impls
        val window = (Gdx.graphics as Lwjgl3Graphics).window
        lastWindowListener = window.windowListener
        window.windowListener = editor
    }

    override fun hide() {
        super.hide()
        main.inputMultiplexer.removeProcessor(processor)
        (Gdx.graphics as Lwjgl3Graphics).window.windowListener = lastWindowListener
    }

    override fun showTransition() {
        super.showTransition()
        resize(Gdx.graphics.width, Gdx.graphics.height)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        editor.resize()
    }
    
    override fun dispose() {
        editor.dispose()
    }
}