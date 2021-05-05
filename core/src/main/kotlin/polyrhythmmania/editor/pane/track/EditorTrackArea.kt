package polyrhythmmania.editor.pane.track

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import io.github.chrislo27.paintbox.ui.*
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.border.SolidBorder
import io.github.chrislo27.paintbox.util.gdxutils.drawRect
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import polyrhythmmania.editor.Click
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.pane.EditorPane


class EditorTrackArea(val allTracksPane: AllTracksPane) : Pane() {
    val editorPane: EditorPane = allTracksPane.editorPane
    val trackView: TrackView = allTracksPane.trackView
    val editor: Editor = editorPane.editor

    private val lastMouseAbsolute: Vector2 = Vector2()
    private val lastMouseRelative: Vector2 = Vector2()

    init {
        this.doClipping.set(true)
        this.border.set(Insets(0f, 2f, 0f, 0f))
        this.borderStyle.set(SolidBorder().apply { this.color.bind { editorPane.palette.trackPaneBorder.use() } })

        this += VerticalBeatLinesPane(editorPane)
    }

    init {
        addInputEventListener { event ->
            when (event) {
                is MouseMoved -> {
                    onMouseMovedOrDragged(event.x, event.y)
                    true
                }
                is TouchDragged -> {
                    onMouseMovedOrDragged(event.x, event.y)
                    true
                }
                else -> false
            }
        }
        editor.trackView.beat.addListener {
            onMouseMovedOrDragged(lastMouseAbsolute.x, lastMouseAbsolute.y)
        }
    }
    
    private fun onMouseMovedOrDragged(x: Float, y: Float) {
        lastMouseAbsolute.set(x, y)
        val thisPos = this.getPosRelativeToRoot(lastMouseRelative)
        lastMouseRelative.x = x - thisPos.x
        lastMouseRelative.y = y - thisPos.y

        val trackY = getTrackFromRelative(lastMouseRelative.y)
        editor.click.getOrCompute().onMouseMoved(getBeatFromRelative(lastMouseRelative.x), trackY.toInt(), trackY)
    }

    fun getTrackFromRelative(relY: Float = lastMouseRelative.y): Float {
        return relY / allTracksPane.editorTrackHeight
    }

    fun getBeatFromRelative(relX: Float = lastMouseRelative.x): Float {
        return editor.trackView.translateXToBeat(relX)
    }
    
    fun trackToRenderY(originY: Float, track: Int): Float {
        val renderBounds = this.contentZone
        val y = originY - renderBounds.y.getOrCompute()
        return y - (track * allTracksPane.editorTrackHeight) - allTracksPane.editorTrackHeight
    }
    
    fun beatToRenderX(originX: Float, beat: Float): Float {
        val renderBounds = this.contentZone
        val x = renderBounds.x.getOrCompute() + originX
        return x + (trackView.translateBeatToX(beat))
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
        super.renderSelfAfterChildren(originX, originY, batch)

        val renderBounds = this.contentZone
        val x = renderBounds.x.getOrCompute() + originX
        val y = originY - renderBounds.y.getOrCompute()
        val w = renderBounds.width.getOrCompute()
        val h = renderBounds.height.getOrCompute()
        val trackView = editor.trackView
        val click = editor.click.getOrCompute()
        val lastPackedColor = batch.packedColor
        val tmpColor = ColorStack.getAndPush()

        // FIXME refactor out?

        batch.setColor(1f, 0f, 0f, 1f)
        batch.fillRect(lastMouseRelative.x + (this.bounds.x.getOrCompute() + originX),
                (originY - this.bounds.y.getOrCompute()) - lastMouseRelative.y,
                5f, 5f)

        if (click is Click.DragSelection) {
            batch.setColor(1f, 1f, 0f, 1f) // yellow
            click.regions.entries.forEach { (block, region) ->
                val renderX = beatToRenderX(originX, region.beat)
                batch.drawRect(renderX,
                        trackToRenderY(originY, region.track),
                        beatToRenderX(originX, region.beat + block.width) - renderX,
                        allTracksPane.editorTrackHeight, 2f)
            }
        }
        
        ColorStack.pop()
        batch.packedColor = lastPackedColor
    }
}