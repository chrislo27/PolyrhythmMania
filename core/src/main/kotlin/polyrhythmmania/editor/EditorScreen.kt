package polyrhythmmania.editor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.ui.SceneRoot
import paintbox.ui.UIElement
import polyrhythmmania.PRManiaGame
import polyrhythmmania.PRManiaScreen
import polyrhythmmania.achievements.Achievements
import polyrhythmmania.statistics.GlobalStats
import polyrhythmmania.storymode.screen.EarlyAccessMsgOnBottom
import java.util.*


class EditorScreen(
    main: PRManiaGame,
    val editorFlags: EnumSet<EditorSpecialFlags>,
    val editorSpecialParams: EditorSpecialParams,
) : PRManiaScreen(main), EarlyAccessMsgOnBottom {

    val batch: SpriteBatch = main.batch
    val editor: Editor = Editor(main, editorFlags, editorSpecialParams)
    private val sceneRoot: SceneRoot = editor.sceneRoot
    private val processor: InputProcessor = editor

    private var lastWindowListener: Lwjgl3WindowListener? = null
    
    constructor(main: PRManiaGame) : this(main, EnumSet.noneOf(EditorSpecialFlags::class.java), EditorSpecialParams())
    
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
        
        GlobalStats.updateEditorPlayTime()
    }
    
    private fun countChildren(element: UIElement): Int {
        val children = element.children.getOrCompute()
        return 1 + children.size + children.sumOf { countChildren(it) }
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

        // Persist statistics semi-regularly; the editor screen opens each time it is cleared/a level is loaded
        GlobalStats.persist()
        Achievements.persist()
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

    override fun shouldMakeTextTransparent(): Boolean = true
}