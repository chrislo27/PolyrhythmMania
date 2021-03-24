package io.github.chrislo27.paintbox.tests.newui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import io.github.chrislo27.paintbox.PaintboxGame
import io.github.chrislo27.paintbox.PaintboxScreen
import io.github.chrislo27.paintbox.PaintboxSettings
import io.github.chrislo27.paintbox.ResizeAction
import io.github.chrislo27.paintbox.logging.Logger
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.util.MathHelper
import io.github.chrislo27.paintbox.util.Version
import io.github.chrislo27.paintbox.util.WindowSize
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import java.io.File


internal class NewUITestGame(paintboxSettings: PaintboxSettings)
    : PaintboxGame(paintboxSettings) {

    override fun getTitle(): String {
        return "New UI test"
    }

    override fun create() {
        super.create()
        this.setScreen(UIAnchorTestEditorScreen(this))
    }
}

internal class UIAnchorTestNestedScreen(override val main: NewUITestGame) : PaintboxScreen() {

    val root: SceneRoot = SceneRoot(Gdx.graphics.width, Gdx.graphics.height)
    val firstEle: UIElement = TestColorElement(Color.CHARTREUSE)
    val secondEle: UIElement = TestColorElement(Color.PURPLE)
    val thirdEle: UIElement = TestColorElement(Color.CYAN)

    init {
        root += firstEle.apply {
            this.bounds.x.set(100f)
            this.bounds.y.set(100f)
            this.bounds.width.set(500f)
            this.bounds.height.set(500f)

            addChild(secondEle.apply {
                this.bounds.x.set(50f)
                this.bounds.y.set(50f)
                this.bounds.width.set(300f)
                this.bounds.height.set(300f)

                addChild(thirdEle.apply {
                    this.bounds.x.set(25f)
                    this.bounds.y.set(25f)
                    this.bounds.width.set(100f)
                    this.bounds.height.set(70f)
                })
            })
        }
    }

    override fun render(delta: Float) {
        super.render(delta)

        if (Gdx.input.isKeyPressed(Input.Keys.F)) {
            firstEle.bounds.x.set(MathUtils.lerp(100f, 600f, MathHelper.getSineWave(1.5f)))
            secondEle.bounds.x.set(MathUtils.lerp(50f, 250f, MathHelper.getSineWave(1.5f)))
            secondEle.bounds.y.set(MathUtils.lerp(0f, 100f, MathHelper.getSineWave(1.5f)))
            thirdEle.bounds.x.set(MathUtils.lerp(25f, 125f, MathHelper.getSineWave(1.125f)))
            thirdEle.bounds.y.set(MathUtils.lerp(0f, 50f, MathHelper.getSineWave(0.75f)))
        } else {
            firstEle.bounds.x.set(100f)
            secondEle.bounds.x.set(50f)
            secondEle.bounds.y.set(50f)
            thirdEle.bounds.x.set(25f)
            thirdEle.bounds.y.set(25f)
        }

        val batch = main.batch
        batch.projectionMatrix = main.nativeCamera.combined
        batch.begin()

        root.renderAsRoot(batch)

        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        root.resize(width.toFloat(), height.toFloat())
    }

    override fun dispose() {
    }
}

internal class TestColorElement(val color: Color) : UIElement() {
    companion object {
        private val TMP_COLOR: Color = Color(1f, 1f, 1f, 1f)
    }

    private var isMouseDown = false
    private var isMouseHovering = false
    var doClickFlash = false

    init {
        addInputEventListener { evt ->
            if (!doClickFlash) return@addInputEventListener false
            if (evt is ClickPressed) {
                if (evt.button == Input.Buttons.LEFT) {
                    Gdx.app.postRunnable { 
                        isMouseDown = true
                    }
                }
                true
            } else if (evt is ClickReleased) {
                if (evt.button == Input.Buttons.LEFT) {
                    Gdx.app.postRunnable {
                        isMouseDown = false
                    }
                }
                true
            } else false
        }
        addInputEventListener { evt ->
            if (!doClickFlash) return@addInputEventListener false
            if (evt is MouseEntered) {
                isMouseHovering = true
                true
            } else if (evt is MouseExited) {
                isMouseHovering = false
                true
            } else false
        }
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val x = bounds.x.getOrCompute() + originX
        val y = originY - bounds.y.getOrCompute()
        val w = bounds.width.getOrCompute()
        val h = bounds.height.getOrCompute()
        val packed = batch.packedColor
        TMP_COLOR.set(color)
        if (isMouseDown) {
            TMP_COLOR.set(1f - color.r, 1f - color.g, 1f - color.b, color.a)
        } else if (isMouseHovering) {
            TMP_COLOR.set(color.r + 0.3f, color.g + 0.3f, color.b + 0.3f, color.a)
        }
        batch.color = TMP_COLOR
        batch.fillRect(x, y - h, w, h)
        batch.packedColor = packed
    }
}
    