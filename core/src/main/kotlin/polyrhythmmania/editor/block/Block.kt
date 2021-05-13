package polyrhythmmania.editor.block

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Align
import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.font.TextAlign
import io.github.chrislo27.paintbox.font.TextBlock
import io.github.chrislo27.paintbox.font.TextRun
import io.github.chrislo27.paintbox.util.gdxutils.drawCompressed
import io.github.chrislo27.paintbox.util.gdxutils.drawRect
import io.github.chrislo27.paintbox.util.gdxutils.fillRect
import io.github.chrislo27.paintbox.util.gdxutils.scaleMul
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.pane.track.EditorTrackArea
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import java.util.*


/**
 * A [Block] represents a timed event or series of events that is placed and manipulated in the editor.
 *
 * A [Block] has a temporal position, represented by [beat], and a visual non-zero [width]. It also has
 * a [trackIndex] which represents what track index the [Block] sits on.
 */
abstract class Block(val editor: Editor, blockTypes: EnumSet<BlockType>) {

    var beat: Float = 0f
    var width: Float = 1f
    var trackIndex: Int = 0
    val blockTypes: Set<BlockType> = blockTypes
    protected val defaultText: Var<String> = Var("")
    protected val defaultTextBlock: Var<TextBlock> = Var.bind {
        editor.blockMarkup.parse(defaultText.use())
    }

    open fun render(batch: SpriteBatch, trackView: TrackView, editorTrackArea: EditorTrackArea,
                    offsetX: Float, offsetY: Float, trackHeight: Float, trackTint: Color) {
        defaultRender(batch, trackView, editorTrackArea, offsetX, offsetY, trackHeight, trackTint)
    }

    protected fun defaultRender(batch: SpriteBatch, trackView: TrackView, editorTrackArea: EditorTrackArea,
                                offsetX: Float, offsetY: Float, trackHeight: Float, trackTint: Color) {
        val renderX = editorTrackArea.beatToRenderX(offsetX, this.beat)
        batch.setColor(trackTint.r, trackTint.g, trackTint.b, 1f)
        batch.fillRect(renderX, editorTrackArea.trackToRenderY(offsetY, trackIndex) - trackHeight,
                editorTrackArea.beatToRenderX(offsetX, this.beat + width) - renderX,
                trackHeight)
        val border = 4f
        batch.setColor(trackTint.r * 0.7f, trackTint.g * 0.7f, trackTint.b * 0.7f, 1f)
        batch.drawRect(renderX, editorTrackArea.trackToRenderY(offsetY, trackIndex) - trackHeight,
                editorTrackArea.beatToRenderX(offsetX, this.beat + width) - renderX,
                trackHeight, border)

        val text = defaultTextBlock.getOrCompute()
        if (text.runs.isNotEmpty()) {
            if (text.isRunInfoInvalid()) {
                // Prevents flickering when drawing on first frame due to bounds not being computed yet
                text.computeLayouts()
            }
            batch.setColor(1f, 1f, 1f, 1f)
            val textPadding = border + 2f
            val scale = 0.9f
            text.drawCompressed(batch, renderX + textPadding,
                    editorTrackArea.trackToRenderY(offsetY, trackIndex) - text.firstCapHeight - textPadding - 1f,
                    editorTrackArea.beatToRenderX(offsetX, this.beat + width) - renderX - textPadding * 2f,
                    TextAlign.LEFT, scale, scale)
        }
//        val text = defaultText.getOrCompute()
//        if (text.isNotEmpty()) {
//            val textPadding = border + 2f
//            editor.main.mainFontBoldBordered.useFont { font ->
//                font.scaleMul(0.9f)
//                font.drawCompressed(batch, text, renderX + textPadding,
//                        editorTrackArea.trackToRenderY(offsetY, trackIndex) - textPadding - 1f,
//                        editorTrackArea.beatToRenderX(offsetX, this.beat + width) - renderX - textPadding * 2, Align.left)
//            }
//        }

        if (editor.selectedBlocks[this] == true) {
            batch.setColor(0.1f, 1f, 1f, 0.333f)
            batch.fillRect(renderX, editorTrackArea.trackToRenderY(offsetY, trackIndex) - trackHeight,
                    editorTrackArea.beatToRenderX(offsetX, this.beat + width) - renderX,
                    trackHeight)
        }
    }
    
    abstract fun compileIntoEvents(): List<Event>

    abstract fun copy(): Block

    protected fun copyBaseInfoTo(target: Block) {
        val from: Block = this
        target.beat = from.beat
        target.width = from.width
        target.trackIndex = from.trackIndex
    }

}