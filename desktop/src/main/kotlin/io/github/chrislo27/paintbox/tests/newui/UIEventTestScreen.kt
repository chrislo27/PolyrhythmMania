package io.github.chrislo27.paintbox.tests.newui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import io.github.chrislo27.paintbox.PaintboxScreen
import io.github.chrislo27.paintbox.ui.Anchor
import io.github.chrislo27.paintbox.ui.SceneRoot
import io.github.chrislo27.paintbox.ui.UIElement
import io.github.chrislo27.paintbox.util.MathHelper

internal class UIEventTestScreen(override val main: NewUITestGame) : PaintboxScreen() {

    private val camera: OrthographicCamera = OrthographicCamera()
    private var root: SceneRoot = SceneRoot(camera)

    init {
        populate()
    }
    
    private fun populate() {
        root = SceneRoot(camera)
        root += TestColorElement(Color(1f, 165f / 255f, 0.5f, 1f))
        
        fun randomColor(): Color = Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1f)
        
        // Toolbar
        root += TestColorElement(Color(0f, 0f, 0f, 0.5f)).also { toolbar ->
            toolbar.bounds.height.set(40f)
            
            // Left toolbar
            toolbar += UIElement().also { lt ->
                lt.bounds.x.set(4f)
                lt.bounds.y.set(4f)
                lt.bounds.height.set(32f)
                lt.bounds.width.bind { (lt.parent.use()?.bounds?.width?.use() ?: 0f) - 8f }
                
                val num = 6
                (0 until num).forEach { i ->
                    lt += TestColorElement(Color(1f, 1f, 1f, 1f).fromHsv(360f * i / num, 0.9f, 0.8f)).apply {
                        this.bounds.x.set(36f * i)
                        this.bounds.y.set(0f)
                        this.bounds.width.set(32f)
                        this.bounds.height.set(32f)
                    }
                }
            }
            
            // Centre toolbar
            toolbar += UIElement().also { ct ->
                val num = 3
                val buttonWidth = 32f
                val buttonSpacing = 4f
                
                ct.bounds.width.set((buttonWidth + buttonSpacing) * num - buttonSpacing)
                Anchor.TopCentre.configure(ct)
                ct.bounds.y.set(4f)
                ct.bounds.height.set(32f)

                (0 until num).forEach { i ->
                    ct += TestColorElement(if (i == 0) Color.YELLOW else if (i == 1) Color.GREEN else Color.RED).apply {
                        this.bounds.x.set(36f * i)
                        this.bounds.y.set(0f)
                        this.bounds.width.set(32f)
                        this.bounds.height.set(32f)
                    }
                }
            }
            
            // Right toolbar
            toolbar += UIElement().also { rt ->
                rt.bounds.x.set(4f)
                rt.bounds.y.set(4f)
                rt.bounds.height.set(32f)
                rt.bounds.width.bind { (rt.parent.use()?.bounds?.width?.use() ?: 0f) - 8f }

                val num = 6
                (0 until num).forEach { i ->
                    rt += TestColorElement(Color(1f, 1f, 1f, 1f).fromHsv(360f * i / num, 0.9f, 0.8f)).apply {
                        Anchor.TopRight.configure(this, offsetX = -36f * i, offsetY = 0f)
                        this.bounds.width.set(32f)
                        this.bounds.height.set(32f)
                    }
                }
            }
        }
    }

    override fun render(delta: Float) {
        super.render(delta)
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            populate()
        }

        val batch = main.batch
        batch.projectionMatrix = camera.combined
        batch.begin()

        root.renderAsRoot(batch)

        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.update()
        root.resize()
    }

    override fun dispose() {
    }
}