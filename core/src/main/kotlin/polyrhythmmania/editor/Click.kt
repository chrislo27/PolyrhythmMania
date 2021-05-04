package polyrhythmmania.editor

import com.badlogic.gdx.math.Vector2
import io.github.chrislo27.paintbox.util.MathHelper
import polyrhythmmania.editor.track.block.Block
import kotlin.math.ceil
import kotlin.math.floor


sealed class Click {
    object None : Click()

    class CreateSelection(val editor: Editor) : Click()

    class DragSelection(val editor: Editor, val blocks: List<Block>,
                        val mouseOffset: Vector2 = Vector2(0f, 0f),
                        val originBlock: Block = blocks.first())
        : Click() {
        
        data class BlockRegion(var beat: Float, var track: Int)
        
        val regions: Map<Block, BlockRegion> = blocks.associateWith { BlockRegion(it.beat, it.trackIndex) }
        val originalOffsets: Map<Block, BlockRegion> = blocks.associateWith { block ->
            BlockRegion(block.beat - originBlock.beat, block.trackIndex - originBlock.trackIndex) 
        }

        // Represents the last known position from onMouseMoved
        var beat: Float = 0f
        var track: Int = 0

        fun complete() {
            // TODO place (and add if necessary) the blocks
            val beatLines = editor.beatLines
            beatLines.active = false
        }

        override fun onMouseMoved(beat: Float, trackIndex: Int, trackY: Float) {
            this.beat = beat //MathHelper.snapToNearest(beat, editor.snapping.getOrCompute())
            this.track = trackIndex
            
            // Adjust block regions
            // Set origin block
            val snapping = editor.snapping.getOrCompute()
            val originRegion = regions.getValue(originBlock)
            val snapToRight = mouseOffset.x > originBlock.width / 2
            if (snapToRight) {
                val newBeatR = MathHelper.snapToNearest((beat - mouseOffset.x + originBlock.width), snapping)
                originRegion.beat = newBeatR - originBlock.width
            } else {
                val newBeat = MathHelper.snapToNearest((beat - mouseOffset.x), snapping)
                originRegion.beat = newBeat
            }
            originRegion.track = (trackY - mouseOffset.y).toInt()
            
            // Set other blocks relative to origin block
            for (block in blocks) {
                if (block === originBlock) continue
                val region = regions.getValue(block)
                val originalOffset = originalOffsets.getValue(block)
                
                val originalOffsetBeat = originalOffset.beat
                val originalOffsetTrack = originalOffset.track
                
                region.beat = originRegion.beat + originalOffsetBeat
                region.track = originRegion.track + originalOffsetTrack
            }
            
            val beatLines = editor.beatLines
            beatLines.active = true
            beatLines.fromBeat = floor(beat).toInt()
            beatLines.toBeat = ceil(beat).toInt()
        }

        override fun renderUpdate() {
        }

        override fun abortAction() {
            val beatLines = editor.beatLines
            beatLines.active = false
        }
    }

    open fun onMouseMoved(beat: Float, trackIndex: Int, trackY: Float) {

    }

    open fun renderUpdate() {
    }

    open fun abortAction() {
    }
}
