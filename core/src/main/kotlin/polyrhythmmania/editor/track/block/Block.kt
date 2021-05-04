package polyrhythmmania.editor.track.block

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.TrackView
import polyrhythmmania.editor.pane.track.EditorTrackArea
import polyrhythmmania.editor.track.BlockType
import java.util.*


/**
 * A [Block] represents a timed event or series of events that is placed and manipulated in the editor.
 * 
 * A [Block] has a temporal position, represented by [beat], and a visual non-zero [width]. It also has
 * a [trackIndex] which represents what track index the [Block] sits on.
 */
open class Block(val editor: Editor, blockTypes: EnumSet<BlockType>) {
    
    var beat: Float = 0f
    var width: Float = 1f
    var trackIndex: Int = 0
    val blockTypes: Set<BlockType> = blockTypes
    
    open fun render(batch: SpriteBatch, trackView: TrackView, editorTrackArea: EditorTrackArea,
                    offsetX: Float, offsetY: Float) {
        
    }
    
}