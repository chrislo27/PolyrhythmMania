package polyrhythmmania.editor.block

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.eclipsesource.json.JsonObject
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.font.TextBlock
import paintbox.ui.contextmenu.ContextMenu
import paintbox.util.MathHelper
import paintbox.util.gdxutils.drawRect
import paintbox.util.gdxutils.fillRect
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.pane.track.EditorTrackArea
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import java.util.*
import kotlin.Comparator



/**
 * A [Block] represents a timed event or series of events that is placed and manipulated in the editor.
 *
 * A [Block] has a temporal position, represented by [beat], and a visual non-zero [width]. It also has
 * a [trackIndex] which represents what track index the [Block] sits on.
 */
abstract class Block(val engine: Engine, blockTypes: EnumSet<BlockType>) {
    
    companion object {
        private val comparator: Comparator<Block> = Comparator { o1, o2 ->
            val beat = o1.beat.compareTo(o2.beat)
            if (beat == 0) {
                o1.trackIndex.compareTo(o2.trackIndex)
            } else beat
        }
        
        fun getComparator(): Comparator<Block> {
            return comparator
        }
    }

    var beat: Float = 0f
    var width: Float = 1f
    var trackIndex: Int = 0
    val blockTypes: Set<BlockType> = blockTypes
    protected val defaultText: Var<String> = Var("")
    protected val defaultTextSecondLine: Var<String> = Var("")
    protected var firstLineTextAlign: TextAlign = TextAlign.LEFT
    protected var secondLineTextAlign: TextAlign = TextAlign.LEFT
    protected var isDefaultTextBlockInitialized: Boolean = false
    protected lateinit var defaultTextBlock: Var<TextBlock>
    protected lateinit var defaultTextBlockSecondLine: Var<TextBlock>
    protected var textScale: Float = 1f
    
    var ownedContextMenu: ContextMenu? = null

    protected fun defaultRender(editor: Editor, batch: SpriteBatch, trackView: TrackView, editorTrackArea: EditorTrackArea,
                                offsetX: Float, offsetY: Float, trackHeight: Float, trackTint: Color) {
        if (!isDefaultTextBlockInitialized) {
            isDefaultTextBlockInitialized = true
            defaultTextBlock = Var.bind {
                editor.blockMarkup.parse(defaultText.use())
            }
            defaultTextBlockSecondLine = Var.bind { 
                editor.blockMarkup.parse(defaultTextSecondLine.use())
            }
        }
        
        val isSelected = editor.selectedBlocks[this] == true
        
        val lastPackedColor = batch.packedColor
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
        if (isSelected) {
            batch.setColor(0f, 1f, 1f, 0.9f)
            batch.drawRect(renderX, editorTrackArea.trackToRenderY(offsetY, trackIndex) - trackHeight,
                    editorTrackArea.beatToRenderX(offsetX, this.beat + width) - renderX,
                    trackHeight, border)
        }

        val textScale = this.textScale
        val text = defaultTextBlock.getOrCompute()
        if (text.runs.isNotEmpty()) {
            if (text.isRunInfoInvalid()) {
                // Prevents flickering when drawing on first frame due to bounds not being computed yet
                text.computeLayouts()
            }
            
            if (ownedContextMenu != null) {
                batch.setColor(MathHelper.getSineWave(0.5f), 1f, 1f, 1f)
            } else {
                batch.setColor(1f, 1f, 1f, 1f)
            }
            
            val textPadding = border + 2f
            val scale = 0.9f * textScale
            text.drawCompressed(batch, renderX + textPadding,
                    editorTrackArea.trackToRenderY(offsetY, trackIndex) - text.firstCapHeight - textPadding - 1f,
                    editorTrackArea.beatToRenderX(offsetX, this.beat + width) - renderX - textPadding * 2f,
                    firstLineTextAlign, scale, scale, true)
        }
        val text2 = defaultTextBlockSecondLine.getOrCompute()
        if (text2.runs.isNotEmpty()) {
            if (text2.isRunInfoInvalid()) {
                // Prevents flickering when drawing on first frame due to bounds not being computed yet
                text2.computeLayouts()
            }

            if (ownedContextMenu != null) {
                batch.setColor(MathHelper.getSineWave(0.5f), 1f, 1f, 1f)
            } else {
                batch.setColor(1f, 1f, 1f, 1f)
            }

            val textPadding = border + 2f
            val scale = 0.9f * textScale
            text2.drawCompressed(batch, renderX + textPadding,
                    editorTrackArea.trackToRenderY(offsetY, trackIndex) - trackHeight + textPadding + 1f,
                    editorTrackArea.beatToRenderX(offsetX, this.beat + width) - renderX - textPadding * 2f,
                    secondLineTextAlign, scale, scale, true)
        }

        if (isSelected) {
            batch.setColor(0.1f, 1f, 1f, 0.3f)
            batch.fillRect(renderX, editorTrackArea.trackToRenderY(offsetY, trackIndex) - trackHeight,
                    editorTrackArea.beatToRenderX(offsetX, this.beat + width) - renderX,
                    trackHeight)
        }
        
        batch.packedColor = lastPackedColor
    }

    open fun render(editor: Editor, batch: SpriteBatch, trackView: TrackView, editorTrackArea: EditorTrackArea,
                    offsetX: Float, offsetY: Float, trackHeight: Float, trackTint: Color) {
        defaultRender(editor, batch, trackView, editorTrackArea, offsetX, offsetY, trackHeight, trackTint)
    }
    
    abstract fun compileIntoEvents(): List<Event>

    abstract fun copy(): Block
    
    open fun createContextMenu(editor: Editor): ContextMenu? = null

    protected fun copyBaseInfoTo(target: Block) {
        val from: Block = this
        target.beat = from.beat
        target.width = from.width
        target.trackIndex = from.trackIndex
    }
    
    open fun writeToJson(obj: JsonObject) {
        obj.add("b", beat)
        obj.add("w", width)
        obj.add("t", trackIndex)
    }
    
    open fun readFromJson(obj: JsonObject) {
        this.beat = obj.getFloat("b", this.beat).coerceAtLeast(0f)
        this.width = obj.getFloat("w", this.width).coerceAtLeast(0f)
        this.trackIndex = obj.getInt("t", this.trackIndex).coerceAtLeast(0)
    }

}