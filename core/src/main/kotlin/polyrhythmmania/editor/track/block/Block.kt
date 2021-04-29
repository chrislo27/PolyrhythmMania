package polyrhythmmania.editor.track.block

import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.track.BlockType
import java.util.*


/**
 * A [Block] represents a timed event or series of events that is placed and manipulated in the editor.
 * 
 * A [Block] has a temporal position, represented by [beat], and a visual non-zero [width]. It also has
 * a [trackID] which represents what track the [Block] sits on.
 */
open class Block(val editor: Editor, blockTypes: EnumSet<BlockType>) {
    
    var beat: Float = 0f
    var width: Float = 1f
    var trackID: String = ""
    val blockTypes: Set<BlockType> = blockTypes
    
}